# URL Shortener - Implementation Approach

## Interview Strategy: How to Approach This Problem

---

## Phase 1: Clarify Requirements (5 minutes)

### Questions to Ask:

1. **Scope**
   - Just shortening + redirect? Or also analytics, custom URLs, QR codes?
   - Focus on backend design or full-stack?

2. **Scale**
   - How many URLs per day? (millions? billions?)
   - Read-to-write ratio? (typical: 100:1)

3. **Expiration**
   - Do URLs expire? Default policy?
   - What happens to expired URLs?

4. **Duplicate Handling**
   - Same long URL → same short URL?
   - Or always generate new?

5. **Security & Auth**
   - Who can create short URLs? (public? API key?)
   - Any restrictions on domains?

### Expected Answer:
"Focus on core: shorten, redirect, expiration. Read-heavy (100:1). Use API key for creation. Same long URL → same short URL. In-memory storage for demo."

---

## Phase 2: High-Level Design (10 minutes)

### Step 1: Identify Core Components

```
User → Shorten Service → Short URL
Short URL → Redirect Service → Long URL
```

**Key entities:**
- UrlMapping (short ↔ long)
- Analytics (click counts)
- Cache (for fast lookups)

### Step 2: Discuss Key Challenges

**Challenge #1: Generating Unique Short Codes**

**Options:**
1. Random string → check collision
2. **Base62 encoding of auto-increment ID** ✅ (recommended)
3. MD5 hash → truncate (collision risk)

**Recommendation:** Base62 encoding
```
ID: 12345 → Base62: "3D7" → https://short.ly/3D7
```

**Why Base62?**
- Compact: 62^7 = 3.5 trillion URLs
- URL-safe: [A-Za-z0-9]
- No collisions (if ID unique)

---

**Challenge #2: Fast Redirects**

**Problem:** Querying DB on every redirect is slow

**Solution:** Cache-first lookup
```
1. Check cache (Redis)
2. If miss → query DB
3. Store in cache
4. Return long URL
```

**Latency:**
- Cache hit: ~1ms
- DB query: ~10-50ms

---

**Challenge #3: Analytics Without Slowing Redirects**

**Problem:** Updating DB adds latency

**Solution:** Asynchronous processing
```
1. Return redirect immediately
2. Fire-and-forget: queue analytics event
3. Background worker processes queue
4. Batch update DB every N seconds
```

---

### Step 3: Draw Simple Architecture

```
Client → Load Balancer → API Server
                              ↓
                    ┌────────┴────────┐
                    ▼                 ▼
                Cache (Redis)    Database
                    ↓
            Analytics Queue → Worker
```

**Explain:**
- "API servers are stateless, can scale horizontally"
- "Cache reduces DB load by 90%+"
- "Analytics processed asynchronously"

---

## Phase 3: Low-Level Design (15 minutes)

### Step 1: Define Classes

**Start with main models:**

```java
// 1. UrlMapping
class UrlMapping {
    long id;
    String shortCode;      // "abc123"
    String longUrl;
    String longUrlHash;    // For duplicate detection
    LocalDateTime expiryTime;
    UrlStatus status;
    long clickCount;
}

// 2. User (optional)
class User {
    String apiKey;
    int dailyLimit;
}
```

**Mention:**
- "Using hash for duplicate detection - faster than comparing full URLs"
- "Status enum: ACTIVE, EXPIRED, DELETED"

---

### Step 2: Service Layer

```java
// 3. UrlService
class UrlService {
    Map<String, UrlMapping> urlsByShortCode;
    Map<String, UrlMapping> urlsByHash;  // Deduplication

    UrlMapping createShortUrl(longUrl, expiryTime);
    UrlMapping getByShortCode(shortCode);
}

// 4. RedirectService
class RedirectService {
    String redirect(shortCode);  // Returns long URL
    // Uses cache-first lookup
}

// 5. AnalyticsService
class AnalyticsService {
    BlockingQueue<Event> eventQueue;
    void recordClickAsync(shortCode);
    // Background thread processes queue
}
```

---

### Step 3: Key Algorithm - Base62 Encoding

**Implement on whiteboard:**

```java
class Base62Encoder {
    static final String CHARS = "0-9A-Za-z";  // 62 chars

    String encode(long id) {
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            sb.append(CHARS.charAt(id % 62));
            id /= 62;
        }
        return sb.reverse().toString();
    }
}
```

**Example:**
```
encode(125)    = "21"
encode(12345)  = "3D7"
encode(1000000) = "4c92"
```

**Explain:**
"Similar to converting decimal to hexadecimal, but base 62 instead of base 16"

---

### Step 4: Duplicate Detection

```java
// Before creating new short URL:

String hash = SHA256(longUrl);

UrlMapping existing = urlsByHash.get(hash);

if (existing != null && !existing.isExpired()) {
    return existing;  // Return existing short code
}

// Otherwise, create new
```

**Explain:**
"This ensures same URL always gets same short code. Uses hash for fast lookup."

---

## Phase 4: Implementation (25-30 minutes)

### Implementation Order:

**1. Enums (2 min)**
```java
enum UrlStatus { ACTIVE, EXPIRED, DELETED }
```

**2. Utils (8 min)**
- `Base62Encoder` ← **Most important!**
- `IdGenerator` (simple AtomicLong)
- `HashUtil` (SHA-256)

**3. Models (5 min)**
- `UrlMapping`
- `User` (optional)

**4. Services (10 min)**
- `UrlService` (create, get)
- `RedirectService` (redirect with cache)
- `AnalyticsService` (async processing)
- `CacheService` (in-memory map)

**5. Main.java Demo (5 min)**
- Create short URLs
- Redirect
- Show analytics
- Test expiration

---

## Phase 5: Demo & Discussion (5 minutes)

### Walk Through Demo

**Run Main.java:**
```bash
java -cp out com.lld.urlshortener.Main
```

**Explain output:**
1. "Created short URL: abc123 for longUrl"
2. "Base62 encoding: ID 12345 → abc123"
3. "Redirect: abc123 → returns long URL"
4. "Analytics: Click count updated asynchronously"
5. "Expiration: Expired URL returns 410 Gone"

---

### Discuss Improvements

**If asked: "How would you scale this?"**

**You:**
1. **Database:**
   - "Use read replicas for redirect traffic"
   - "Shard by shortCode hash for write distribution"

2. **Cache:**
   - "Distributed cache (Redis Cluster)"
   - "Cache warm-up for popular URLs"

3. **ID Generation:**
   - "Twitter Snowflake for distributed IDs"
   - "Avoids single point of failure"

4. **Analytics:**
   - "Message queue (Kafka) instead of in-memory queue"
   - "Separate analytics microservice"

5. **CDN:**
   - "Edge caching for global low-latency"
   - "CloudFlare/Akamai"

---

## Common Mistakes to Avoid

### ❌ DON'T:

1. **Use MD5/SHA hash as short code**
   - Too long (even truncated)
   - Collision risk
   - Not sequential

2. **Query DB on every redirect**
   - Slow!
   - Cache-first is essential

3. **Update analytics synchronously**
   - Adds 10-20ms latency
   - Use async queue

4. **Forget expiration check**
   - Expired URLs should return 410 Gone
   - Critical requirement

5. **Ignore duplicate URLs**
   - Same URL → new short code each time?
   - Hash-based dedup is expected

---

### ✅ DO:

1. **Use Base62 encoding**
   - Compact
   - No collisions
   - URL-safe

2. **Implement cache-first lookup**
   - Shows you understand performance

3. **Async analytics**
   - Demonstrates good architecture

4. **Handle edge cases:**
   - Expired URLs
   - Invalid URLs
   - Duplicate URLs

5. **Explain scalability**
   - Sharding
   - Read replicas
   - Distributed cache

---

## Time Management (60-min interview)

| Phase | Time | Focus |
|-------|------|-------|
| Requirements | 5 min | Clarify scope, scale |
| HLD | 10 min | Architecture, challenges |
| LLD | 15 min | Classes, algorithms |
| **Base62 Encoder** | 8 min | **Critical!** Code it well |
| Other Services | 12 min | UrlService, RedirectService |
| Demo | 5 min | Show it works |
| Discussion | 5 min | Scalability, trade-offs |

**Tip:** If running low on time, **prioritize Base62 Encoder** - it's the most interview-critical part!

---

## Key Talking Points

### When Implementing Base62:

**You:** "I'm using Base62 encoding to convert numeric IDs into short alphanumeric codes. This gives us 62^7 = 3.5 trillion possible URLs with just 7 characters. It's similar to converting decimal to hexadecimal, but base 62 uses 0-9, A-Z, a-z."

**[Code it on whiteboard]**

---

### When Discussing Cache:

**You:** "I'm using cache-first lookup pattern. Check cache first, fallback to DB on miss. This reduces database load by 90%+ since redirects are much more frequent than creates. In production, we'd use Redis with TTL set to URL expiry time."

---

### When Discussing Analytics:

**You:** "Analytics are processed asynchronously to avoid slowing redirects. Events go into a queue, background worker processes them in batches, and updates DB every few seconds. This keeps redirect latency under 10ms."

---

### When Discussing Duplicate URLs:

**You:** "I hash the long URL and store it for deduplication. Before creating a new short URL, I check if this hash already exists. If yes and not expired, return the existing short code. This ensures same URL → same short code, which saves storage and makes sense from UX perspective."

---

## What Interviewers Look For

### ✅ Strong Candidates:

- Understand Base62 encoding clearly
- Implement cache-first lookup
- Handle async analytics properly
- Discuss sharding and replication
- Code clean, working Base62 encoder
- Explain trade-offs

### ❌ Weak Candidates:

- Use random strings (collision risk)
- Query DB on every redirect
- Update analytics synchronously
- Don't handle expiration
- Can't code Base62 encoder
- Don't discuss scalability

---

## Practice Checklist

Before interview:
- [ ] Can you code Base62 encoder in 5 minutes?
- [ ] Can you explain cache-first lookup?
- [ ] Can you explain async analytics?
- [ ] Do you know 62^7 = 3.5 trillion?
- [ ] Can you draw HLD architecture?

---

## Final Tips

1. **Start with Base62 encoding**
   - Most interviewers expect this
   - Shows mathematical thinking

2. **Emphasize cache**
   - Critical for performance
   - Shows system design awareness

3. **Don't over-engineer**
   - Focus on core: shorten + redirect
   - Mention advanced features verbally

4. **Code Base62 encoder well**
   - It's the signature algorithm
   - Practice until fluent

---

**YOU'RE READY!** 🚀

Implement Base62, use cache, async analytics, handle expiration.

That's all you need for a strong interview! 💪
