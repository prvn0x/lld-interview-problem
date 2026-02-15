# URL Shortener - Implementation

## 📝 Problem Statement

Design a URL shortening service (like bit.ly, TinyURL) that:
- Converts long URLs into short, easy-to-share URLs
- Redirects short URLs back to original URLs
- Tracks analytics (click counts)
- Handles URL expiration
- Supports high read traffic with caching

---

## 🎯 Features Implemented

### ✅ Core Features
1. **URL Shortening**
   - Base62 encoding for compact codes
   - Unique short codes (6-7 characters)
   - Idempotent (same URL → same short code)

2. **Fast Redirects**
   - Cache-first lookup (<1ms cache hit)
   - Fallback to database on cache miss
   - Expiration validation

3. **Analytics**
   - Asynchronous click tracking
   - Batch processing (doesn't slow redirects)
   - Real-time click counts

4. **URL Expiration**
   - Default: 1 year
   - Custom expiry support
   - Expired URLs return HTTP 410

5. **Duplicate Detection**
   - SHA-256 hash-based deduplication
   - Same long URL → returns existing short code

---

## 🏗️ Architecture

### Components

```
┌──────────────┐
│ UrlService   │  Create/manage short URLs
└──────┬───────┘
       │
       ▼
┌──────────────────────────────┐
│  RedirectService             │  Handle redirects
│  (cache-first lookup)        │
└──────┬───────────────────────┘
       │
       ▼
┌──────────────┐      ┌──────────────┐
│ CacheService │──────│AnalyticsService│
│  (Redis sim) │      │(async queue)  │
└──────────────┘      └───────────────┘
```

### Key Algorithms

**1. Base62 Encoding**
```
ID: 12345 → Base62: "3D7"
62^6 = 56 billion URLs
62^7 = 3.5 trillion URLs
```

**2. Cache-First Lookup**
```
Check cache → Hit? Return
           → Miss? Query DB, cache, return
```

**3. Async Analytics**
```
Redirect request → Return immediately
                → Queue event (non-blocking)
                → Background thread processes batches
```

---

## 🚀 How to Run

### Using IntelliJ IDEA
1. Open project
2. Navigate to `src/com/lld/urlshortener/Main.java`
3. Right-click → **Run 'Main.main()'**

### Using Command Line
```bash
# Compile
javac -d out -sourcepath src src/com/lld/urlshortener/Main.java

# Run
java -cp out com.lld.urlshortener.Main
```

---

## 📊 Demo Flow

The `Main.java` demonstrates:

1. **Base62 Encoding** - Convert IDs to short codes
2. **Create Short URLs** - Generate unique short codes
3. **Duplicate Detection** - Same URL → same short code
4. **Redirect URLs** - Cache-first lookup
5. **Analytics Processing** - Async batch updates
6. **URL Expiration** - Handle expired URLs
7. **Error Handling** - 404 Not Found, 410 Gone
8. **Cache Performance** - Invalidation and refresh
9. **User Statistics** - URLs by user

---

## 🧮 Key Implementation Details

### Base62 Encoder

```java
String encode(long id) {
    StringBuilder sb = new StringBuilder();
    while (id > 0) {
        sb.append(BASE62_CHARS.charAt(id % 62));
        id /= 62;
    }
    return sb.reverse().toString();
}
```

**Why Base62?**
- URL-safe: [0-9A-Za-z]
- Compact: 7 chars = 3.5 trillion URLs
- No collisions (if ID is unique)

### Duplicate Detection

```java
String hash = SHA256(longUrl);
UrlMapping existing = urlsByHash.get(hash);
if (existing != null && !existing.isExpired()) {
    return existing;  // Reuse short code
}
```

### Async Analytics

```java
// Non-blocking - add to queue and return immediately
eventQueue.offer(new AnalyticsEvent(shortCode));

// Background thread processes in batches
processEvents() {
    batch = eventQueue.drainTo(1000);
    aggregate(batch);
    batchUpdate(database);
}
```

---

## 📦 Package Structure

```
urlshortener/
├── enums/
│   └── UrlStatus.java
├── models/
│   ├── UrlMapping.java
│   ├── User.java
│   └── AnalyticsEvent.java
├── utils/
│   ├── Base62Encoder.java     ⭐ Most important!
│   ├── IdGenerator.java
│   └── HashUtil.java
├── cache/
│   └── CacheService.java
├── services/
│   ├── UrlService.java
│   ├── RedirectService.java
│   └── AnalyticsService.java
└── Main.java                    ⭐ Run this!
```

---

## 🎯 Interview Discussion Points

### When Asked: "How does Base62 work?"

**You:** "Base62 encoding converts numeric IDs to alphanumeric strings using 62 characters (0-9, A-Z, a-z). It's like converting decimal to hexadecimal, but base 62 instead of base 16. With 7 characters, we can represent 62^7 = 3.5 trillion unique URLs."

### When Asked: "How do you prevent double-booking?"

**You:** "I hash the long URL using SHA-256 and store the hash. Before creating a new short URL, I check if this hash already exists. If yes and not expired, I return the existing short code. This ensures same URL → same short code."

### When Asked: "How do you handle high traffic?"

**You:** "I use cache-first lookup pattern. Check Redis first (1ms), fallback to DB on miss. This reduces database load by 90%+. For analytics, I use async processing - events go to a queue, background worker processes in batches. This keeps redirect latency under 10ms."

### When Asked: "How would you scale this?"

**You:**
1. **Sharding:** Shard database by short code hash
2. **Read Replicas:** For redirect traffic
3. **Distributed Cache:** Redis Cluster with consistent hashing
4. **Distributed IDs:** Twitter Snowflake instead of auto-increment
5. **CDN:** Edge caching for global low-latency
6. **Message Queue:** Kafka for analytics instead of in-memory queue

---

## 💾 Data Storage

### In-Memory (Demo)
```java
Map<String, UrlMapping> urlsByShortCode;      // shortCode → mapping
Map<String, UrlMapping> urlsByHash;           // hash → mapping
ConcurrentHashMap<String, UrlMapping> cache;  // cache layer
BlockingQueue<AnalyticsEvent> eventQueue;     // analytics
```

### Production (Suggested)
```sql
CREATE TABLE url_mappings (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE,
    long_url TEXT,
    long_url_hash VARCHAR(64),
    created_at TIMESTAMP,
    expiry_time TIMESTAMP,
    status ENUM('ACTIVE', 'EXPIRED'),
    click_count BIGINT,

    INDEX idx_short_code (short_code),
    INDEX idx_hash (long_url_hash)
);
```

---

## 🧪 Test Cases Covered

✅ Create short URL
✅ Duplicate URL → same short code
✅ Redirect valid URL
✅ Redirect expired URL → 410 Gone
✅ Redirect non-existent → 404 Not Found
✅ Analytics async processing
✅ Cache hit/miss
✅ Cache invalidation

---

## 📈 Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| Create URL | O(1) | Hash lookup + insert |
| Redirect (cache hit) | O(1) | ~1ms |
| Redirect (cache miss) | O(1) | ~10-50ms (DB query) |
| Analytics update | O(1) amortized | Batch processing |

### Capacity

```
ID Range: 1 to 10^15
Base62 Length: 6-9 characters
62^7 = 3,521,614,606,208 URLs (3.5 trillion)
```

---

## 🔒 Security Considerations

### Implemented
- URL validation (must start with http/https)
- Rate limiting support (User model has dailyLimit)
- API key authentication structure

### Production TODO
- Blacklist malicious domains
- Rate limiting with Redis
- HTTPS only
- DDoS protection (CDN, WAF)
- Virus scanning for uploaded URLs

---

## 🎓 Learning Points

### Design Patterns
- **Singleton:** IdGenerator
- **Cache-Aside:** CacheService
- **Producer-Consumer:** AnalyticsService queue

### SOLID Principles
- **SRP:** Each service has one responsibility
- **DIP:** Services depend on interfaces (CacheService)

### Concurrency
- AtomicLong for ID generation
- ConcurrentHashMap for thread-safe storage
- BlockingQueue for async analytics

---

## ✅ Checklist for Interview

- [x] Can explain Base62 encoding
- [x] Can code Base62 encoder in 5 minutes
- [x] Understand cache-first lookup
- [x] Know async analytics pattern
- [x] Can discuss sharding strategy
- [x] Can explain duplicate detection

---

**Author:** Praveen Singh
**Date:** February 2026
**Purpose:** LLD Interview Preparation
