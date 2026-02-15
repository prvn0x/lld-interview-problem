# URL Shortener - Low-Level Design (LLD)

## 1. Class Diagram Overview

### Core Entities (Models)
- UrlMapping
- User (API key owner)
- AnalyticsEvent

### Services (Business Logic)
- UrlService
- RedirectService
- AnalyticsService
- CacheService

### Utilities
- Base62Encoder
- IdGenerator
- HashUtil

### Enums
- UrlStatus (ACTIVE, EXPIRED, DELETED)

---

## 2. Detailed Class Design

### 2.1 UrlMapping Class

```java
class UrlMapping {
    - long id                       // Unique ID (auto-increment or Snowflake)
    - String shortCode              // Base62 encoded (e.g., "abc123")
    - String longUrl                // Original URL
    - String longUrlHash            // SHA-256 hash for duplicate detection
    - LocalDateTime createdAt
    - LocalDateTime expiryTime      // null = never expires
    - UrlStatus status              // ACTIVE, EXPIRED, DELETED
    - long clickCount               // Total redirects
    - String createdBy              // API key or user ID

    + UrlMapping(id, shortCode, longUrl)
    + boolean isExpired()
    + void incrementClickCount()
    + String getShortUrl(String baseUrl)  // Returns full URL
    + getters/setters
}
```

### 2.2 User Class

```java
class User {
    - String userId
    - String apiKey                 // For authentication
    - String name
    - LocalDateTime createdAt
    - int dailyUrlLimit             // Rate limiting
    - int urlsCreatedToday

    + User(userId, apiKey, name)
    + boolean canCreateUrl()        // Check rate limit
    + void incrementUrlCount()
    + getters/setters
}
```

### 2.3 AnalyticsEvent Class

```java
class AnalyticsEvent {
    - String shortCode
    - LocalDateTime timestamp
    - String userAgent              // Optional: browser info
    - String ipAddress              // Optional: client IP
    - String referer                // Optional: referrer URL

    + AnalyticsEvent(shortCode, timestamp)
    + getters/setters
}
```

---

## 3. Service Classes (Business Logic)

### 3.1 UrlService

```java
class UrlService {
    - Map<String, UrlMapping> urlsByShortCode
    - Map<String, UrlMapping> urlsByHash     // For deduplication
    - IdGenerator idGenerator
    - Base62Encoder encoder
    - HashUtil hashUtil
    - CacheService cacheService

    + UrlMapping createShortUrl(longUrl, expiryTime, apiKey)
    + UrlMapping getByShortCode(shortCode)
    + boolean deleteUrl(shortCode)
    + List<UrlMapping> getUrlsByUser(apiKey)
    - UrlMapping findExistingUrl(longUrlHash)
    - String generateShortCode(id)
}
```

**Key Methods:**

#### createShortUrl
```java
public UrlMapping createShortUrl(String longUrl, LocalDateTime expiryTime,
                                 String apiKey) {
    // 1. Validate URL
    if (!isValidUrl(longUrl)) {
        throw new InvalidUrlException();
    }

    // 2. Check for duplicate
    String hash = hashUtil.hash(longUrl);
    UrlMapping existing = findExistingUrl(hash);
    if (existing != null && !existing.isExpired()) {
        return existing;  // Return existing short URL
    }

    // 3. Generate unique ID
    long id = idGenerator.generateId();

    // 4. Encode to Base62
    String shortCode = encoder.encode(id);

    // 5. Create mapping
    UrlMapping mapping = new UrlMapping(id, shortCode, longUrl);
    mapping.setLongUrlHash(hash);
    mapping.setExpiryTime(expiryTime);
    mapping.setCreatedBy(apiKey);

    // 6. Store
    urlsByShortCode.put(shortCode, mapping);
    urlsByHash.put(hash, mapping);

    // 7. Cache
    cacheService.put(shortCode, mapping);

    return mapping;
}
```

---

### 3.2 RedirectService

```java
class RedirectService {
    - UrlService urlService
    - CacheService cacheService
    - AnalyticsService analyticsService

    + String redirect(shortCode)
    + void recordClick(shortCode)
    - UrlMapping lookupUrl(shortCode)
    - void validateExpiry(UrlMapping mapping)
}
```

**Key Method: redirect**

```java
public String redirect(String shortCode) {
    // 1. Lookup (cache-first)
    UrlMapping mapping = lookupUrl(shortCode);

    if (mapping == null) {
        throw new NotFoundException("Short URL not found");
    }

    // 2. Check expiration
    if (mapping.isExpired()) {
        throw new UrlExpiredException("Short URL has expired");
    }

    // 3. Record analytics asynchronously
    analyticsService.recordClickAsync(shortCode);

    // 4. Return long URL
    return mapping.getLongUrl();
}

private UrlMapping lookupUrl(String shortCode) {
    // Check cache first
    UrlMapping cached = cacheService.get(shortCode);
    if (cached != null) {
        return cached;
    }

    // Cache miss - query service
    UrlMapping mapping = urlService.getByShortCode(shortCode);
    if (mapping != null) {
        cacheService.put(shortCode, mapping);
    }

    return mapping;
}
```

---

### 3.3 AnalyticsService

```java
class AnalyticsService {
    - BlockingQueue<AnalyticsEvent> eventQueue
    - Map<String, Long> clickCounts          // In-memory aggregation
    - UrlService urlService
    - Thread processingThread

    + void recordClickAsync(shortCode)
    + void recordClickAsync(shortCode, userAgent, ipAddress)
    + long getClickCount(shortCode)
    - void processEvents()                   // Background thread
    - void batchUpdateDatabase()
}
```

**Asynchronous Processing:**

```java
public void recordClickAsync(String shortCode) {
    AnalyticsEvent event = new AnalyticsEvent(shortCode, LocalDateTime.now());

    // Non-blocking: add to queue
    eventQueue.offer(event);

    // Fire-and-forget
}

// Background thread
private void processEvents() {
    while (true) {
        try {
            // Batch processing
            List<AnalyticsEvent> batch = new ArrayList<>();
            eventQueue.drainTo(batch, 1000);  // Process 1000 at a time

            if (!batch.isEmpty()) {
                // Aggregate by shortCode
                Map<String, Long> counts = batch.stream()
                    .collect(Collectors.groupingBy(
                        AnalyticsEvent::getShortCode,
                        Collectors.counting()
                    ));

                // Update in-memory counts
                counts.forEach((code, count) -> {
                    clickCounts.merge(code, count, Long::sum);
                });

                // Persist to DB (batch update)
                batchUpdateDatabase(counts);
            }

            Thread.sleep(5000);  // Process every 5 seconds
        } catch (InterruptedException e) {
            break;
        }
    }
}
```

---

### 3.4 CacheService

```java
class CacheService {
    - Map<String, UrlMapping> cache          // In-memory cache (simulate Redis)
    - Map<String, LocalDateTime> expiryTimes

    + void put(shortCode, urlMapping)
    + UrlMapping get(shortCode)
    + void invalidate(shortCode)
    + void evictExpired()                    // Periodic cleanup
}
```

**Cache Implementation:**

```java
public void put(String shortCode, UrlMapping mapping) {
    cache.put(shortCode, mapping);

    // Set TTL based on URL expiry
    if (mapping.getExpiryTime() != null) {
        expiryTimes.put(shortCode, mapping.getExpiryTime());
    } else {
        // Default TTL: 1 year
        expiryTimes.put(shortCode, LocalDateTime.now().plusYears(1));
    }
}

public UrlMapping get(String shortCode) {
    // Check if expired
    LocalDateTime expiry = expiryTimes.get(shortCode);
    if (expiry != null && expiry.isBefore(LocalDateTime.now())) {
        // Expired - remove from cache
        cache.remove(shortCode);
        expiryTimes.remove(shortCode);
        return null;
    }

    return cache.get(shortCode);
}
```

---

## 4. Utility Classes

### 4.1 Base62Encoder

```java
class Base62Encoder {
    - static final String BASE62_CHARS =
        "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    + String encode(long id)
    + long decode(String shortCode)
}
```

**Implementation:**

```java
public String encode(long id) {
    if (id == 0) return "0";

    StringBuilder sb = new StringBuilder();
    while (id > 0) {
        int remainder = (int) (id % 62);
        sb.append(BASE62_CHARS.charAt(remainder));
        id = id / 62;
    }

    return sb.reverse().toString();
}

public long decode(String shortCode) {
    long id = 0;
    for (char c : shortCode.toCharArray()) {
        id = id * 62 + BASE62_CHARS.indexOf(c);
    }
    return id;
}
```

**Examples:**
```
encode(125)     → "21"
encode(12345)   → "3D7"
encode(9876543) → "aI1Z"
```

---

### 4.2 IdGenerator

```java
class IdGenerator {
    - AtomicLong counter          // Simulates auto-increment

    + long generateId()
}
```

**Simple Implementation:**
```java
public long generateId() {
    return counter.incrementAndGet();
}
```

**Production:** Use Twitter Snowflake or database auto-increment.

---

### 4.3 HashUtil

```java
class HashUtil {
    + String hash(String input)   // SHA-256
    + String hashMD5(String input)
}
```

**Implementation:**

```java
public String hash(String input) {
    try {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(input.getBytes());

        // Convert to hex string
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
    }
}
```

---

## 5. Enums

### UrlStatus

```java
enum UrlStatus {
    ACTIVE,      // URL is valid and active
    EXPIRED,     // URL has passed expiry time
    DELETED      // URL manually deleted by user
}
```

---

## 6. Key Algorithms

### 6.1 Short Code Generation

**Process:**
```
1. Generate unique ID: 12345678
2. Encode to Base62: "aI1Z"
3. Short URL: https://short.ly/aI1Z
```

**Base62 vs Base10:**
```
Base10: 12345678 → "12345678" (8 chars)
Base62: 12345678 → "aI1Z"     (4 chars)
```

**Capacity:**
```
6 chars → 62^6 = 56 billion URLs
7 chars → 62^7 = 3.5 trillion URLs
```

---

### 6.2 Duplicate Detection

**Problem:** Same long URL submitted twice

**Solution:**
```java
// 1. Hash long URL
String hash = SHA256(longUrl);

// 2. Check if hash exists
UrlMapping existing = urlsByHash.get(hash);

// 3. If exists and not expired → return existing
if (existing != null && !existing.isExpired()) {
    return existing.getShortCode();
}

// 4. Otherwise, create new
```

**Why hash instead of direct lookup?**
- Long URLs can be very long (> 2000 chars)
- Hash is fixed size (64 chars for SHA-256)
- Faster indexing and comparison

---

### 6.3 Cache-First Lookup

**Flow:**
```
┌─────────────────────┐
│ Redirect Request    │
│   /abc123           │
└──────────┬──────────┘
           │
           ▼
    ┌──────────────┐
    │ Check Cache  │
    └──────┬───────┘
           │
      ┌────┴────┐
      │  HIT?   │
      └────┬────┘
      YES  │  NO
      ▼    │    ▼
   ┌─────┐ │ ┌─────────┐
   │Cache│ │ │Database │
   │Value│ │ │ Lookup  │
   └─────┘ │ └────┬────┘
           │      │
           │      ▼
           │ ┌─────────┐
           │ │Store in │
           │ │ Cache   │
           │ └─────────┘
           │      │
           ▼      ▼
      ┌──────────────┐
      │Return longUrl│
      └──────────────┘
```

---

## 7. Design Patterns Used

### 7.1 Singleton Pattern

**Where:** IdGenerator, CacheService

**Why:** Single shared instance for ID generation and caching

```java
class IdGenerator {
    private static IdGenerator instance;

    private IdGenerator() {}

    public static IdGenerator getInstance() {
        if (instance == null) {
            instance = new IdGenerator();
        }
        return instance;
    }
}
```

---

### 7.2 Factory Pattern

**Where:** UrlMapping creation

**Why:** Centralize object creation logic

```java
class UrlMappingFactory {
    public static UrlMapping create(long id, String longUrl,
                                    LocalDateTime expiryTime) {
        String shortCode = Base62Encoder.encode(id);
        String hash = HashUtil.hash(longUrl);

        UrlMapping mapping = new UrlMapping(id, shortCode, longUrl);
        mapping.setLongUrlHash(hash);
        mapping.setExpiryTime(expiryTime);

        return mapping;
    }
}
```

---

### 7.3 Observer Pattern (Implicit)

**Where:** Analytics event processing

**Why:** Decouple redirect from analytics update

```java
// RedirectService notifies AnalyticsService
redirectService.redirect(shortCode);
  → analyticsService.recordClickAsync(shortCode);  // Fire-and-forget
```

---

## 8. SOLID Principles Applied

### Single Responsibility Principle (SRP)
- `UrlService` → URL creation/management
- `RedirectService` → Redirect logic
- `AnalyticsService` → Analytics processing
- Each class has ONE job

### Open/Closed Principle (OCP)
- Can add new encoding strategies (Base62, Base64, custom)
- Can add new cache implementations (Redis, Memcached)

### Dependency Inversion Principle (DIP)
- Services depend on abstractions (interfaces), not concrete classes
- Example: `CacheService` could be an interface with multiple implementations

---

## 9. Concurrency Considerations

### Thread-Safe Operations

**IdGenerator:**
```java
private AtomicLong counter = new AtomicLong(0);

public long generateId() {
    return counter.incrementAndGet();  // Atomic operation
}
```

**Analytics Queue:**
```java
private BlockingQueue<AnalyticsEvent> eventQueue =
    new LinkedBlockingQueue<>(10000);  // Thread-safe queue
```

**Cache:**
```java
private ConcurrentHashMap<String, UrlMapping> cache =
    new ConcurrentHashMap<>();  // Thread-safe map
```

---

## 10. Error Handling

### Custom Exceptions

```java
class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}

class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String message) {
        super(message);
    }
}

class InvalidUrlException extends RuntimeException {
    public InvalidUrlException(String message) {
        super(message);
    }
}

class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }
}
```

---

## 11. Class Relationships

### UML Diagram (Text)

```
┌──────────────┐
│  UrlService  │
├──────────────┤
│ + createUrl()│
│ + getByCode()│
└──────┬───────┘
       │ uses
       ▼
┌──────────────┐         ┌──────────────┐
│  UrlMapping  │◄────────│Base62Encoder │
├──────────────┤         ├──────────────┤
│ - shortCode  │         │ + encode()   │
│ - longUrl    │         │ + decode()   │
│ - expiryTime │         └──────────────┘
└──────┬───────┘
       │ 1:N
       ▼
┌──────────────┐
│ AnalyticsEvent│
├──────────────┤
│ - shortCode  │
│ - timestamp  │
└──────────────┘

┌────────────────┐
│RedirectService │
├────────────────┤
│ + redirect()   │──────► AnalyticsService
└────────┬───────┘
         │ uses
         ▼
    CacheService
```

---

## Summary

This LLD covers:
- ✅ Complete class design with relationships
- ✅ Service layer architecture
- ✅ Key algorithms (Base62, hashing, caching)
- ✅ Design patterns (Singleton, Factory, Observer)
- ✅ SOLID principles
- ✅ Concurrency handling
- ✅ In-memory data structures

**Next:** See **APPROACH.md** for implementation strategy and then the **Java implementation**.
