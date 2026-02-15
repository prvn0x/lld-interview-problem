# 🎯 URL SHORTENER - INTERVIEW GUIDE

## What You Need to Memorize & Show

---

## 📁 Final Structure

```
urlshortener/
├── HLD.md              # Read: Architecture, APIs, scaling
├── LLD.md              # Read: Classes, algorithms
├── APPROACH.md         # Read: Interview strategy
├── README.md           # Quick reference
├── INTERVIEW_GUIDE.md  # ⭐ THIS FILE - read before interview
│
├── enums/              # UrlStatus
├── models/             # UrlMapping, User, AnalyticsEvent
├── utils/              # Base62Encoder ⭐, IdGenerator, HashUtil
├── cache/              # CacheService
├── services/           # UrlService, RedirectService, AnalyticsService
│
└── Main.java          # ✅ RUN THIS
```

---

## 🧠 MEMORIZE These 3 Things (5 minutes)

### 1️⃣ Base62 Encoding (CRITICAL!)

```java
String encode(long id) {
    StringBuilder sb = new StringBuilder();
    while (id > 0) {
        sb.append(CHARS.charAt(id % 62));
        id /= 62;
    }
    return sb.reverse().toString();
}
```

**Say:** "Base62 uses 62 characters (0-9, A-Z, a-z). With 7 characters, we get 62^7 = 3.5 trillion unique URLs. It's like converting decimal to hexadecimal, but base 62."

### 2️⃣ Cache-First Lookup

```java
UrlMapping cached = cache.get(shortCode);
if (cached != null) return cached;  // Cache hit!

// Cache miss - query database
UrlMapping mapping = database.get(shortCode);
cache.put(shortCode, mapping);
return mapping;
```

**Say:** "Cache-first reduces latency. Cache hit = 1ms, DB query = 10-50ms. Reduces DB load by 90%+."

### 3️⃣ Async Analytics

```java
// Redirect: return immediately
public String redirect(shortCode) {
    String longUrl = lookupUrl(shortCode);
    analyticsQueue.offer(new Event(shortCode));  // Fire-and-forget
    return longUrl;
}

// Background thread: process in batches
while (true) {
    batch = queue.drainTo(1000);
    aggregate(batch);
    batchUpdate(database);
    sleep(5 seconds);
}
```

**Say:** "Analytics processed asynchronously. Redirect returns immediately (<10ms), events queued and processed in batches by background thread."

---

## 🎬 Interview Flow (60 min)

### PHASE 1: Clarify (5 min)

**Ask:**
- "Focus on core (shorten, redirect) or also custom URLs, QR codes?"
- "What's the read-to-write ratio?" (typically 100:1)
- "Should same long URL give same short URL?" (yes - duplicate detection)
- "URL expiration?" (yes - default 1 year)

**Expected:** "Core features, read-heavy, deduplicate, in-memory storage."

---

### PHASE 2: High-Level Design (10 min)

**Draw:**
```
Client → API Server
           ↓
     ┌─────┴──────┐
     ▼            ▼
  Cache        Database
  (Redis)
     ↓
Analytics Queue
```

**Explain key challenges:**

**1. Short Code Generation**
- "Use Base62 encoding of auto-increment ID"
- "ID 12345 → Base62 '3D7'"
- "62^7 = 3.5 trillion URLs"

**2. Fast Redirects**
- "Cache-first lookup"
- "Cache hit = 1ms, DB = 10-50ms"

**3. Analytics Without Slowing Redirects**
- "Async queue + background processing"
- "Redirect returns immediately"

---

### PHASE 3: Low-Level Design (15 min)

**Start with models:**

```java
class UrlMapping {
    long id;
    String shortCode;      // Base62 encoded
    String longUrl;
    String longUrlHash;    // SHA-256 for dedup
    LocalDateTime expiryTime;
    UrlStatus status;
    long clickCount;
}
```

**Then services:**

```java
class UrlService {
    Map<String, UrlMapping> urlsByShortCode;
    Map<String, UrlMapping> urlsByHash;  // Deduplication

    UrlMapping createShortUrl(longUrl, expiryTime);
    // 1. Hash long URL
    // 2. Check duplicate
    // 3. Generate ID → Base62 → short code
    // 4. Store + cache
}

class RedirectService {
    String redirect(shortCode);
    // 1. Cache-first lookup
    // 2. Validate expiry
    // 3. Queue analytics event
    // 4. Return long URL
}

class AnalyticsService {
    BlockingQueue<Event> queue;
    // Background thread processes batches
}
```

---

### PHASE 4: Implementation (30 min)

**Priority order:**

1. **Base62Encoder** (10 min) ⭐ MOST IMPORTANT
   - This is the signature algorithm
   - Practice until fluent!

2. **UrlMapping + IdGenerator** (5 min)

3. **UrlService** (8 min)
   - createShortUrl with duplicate check
   - getByShortCode

4. **CacheService** (3 min)
   - Simple get/put/invalidate

5. **RedirectService** (4 min)
   - Cache-first lookup
   - Expiry validation

6. **AnalyticsService** (if time) (5 min)
   - Queue + background thread

7. **Main.java demo** (5 min)

**If running out of time:**
- Skip AnalyticsService (mention verbally)
- Focus on Base62 + UrlService + RedirectService

---

## 🎯 Key Talking Points

### Base62 Encoding

**Interviewer:** "How do you generate short codes?"

**You:** "I use Base62 encoding. First, generate a unique numeric ID using auto-increment or Snowflake. Then encode it to Base62 using 62 characters (0-9, A-Z, a-z). For example, ID 12345 becomes '3D7'. With 7 characters, we can support 62^7 = 3.5 trillion URLs. It's similar to converting decimal to hexadecimal, but base 62."

[Code it on whiteboard - should take 3-5 minutes]

---

### Duplicate Detection

**Interviewer:** "What if same URL is submitted twice?"

**You:** "I hash the long URL using SHA-256 and store the hash. Before creating a new short URL, I check if this hash already exists. If yes and not expired, I return the existing short code. This ensures same URL → same short code, which makes sense from UX and saves storage."

---

### Cache Strategy

**Interviewer:** "How do you keep redirects fast?"

**You:** "Cache-first lookup pattern. I check Redis first (1ms), fallback to database on miss (10-50ms). After DB query, I store in cache for next time. This reduces database load by 90%+ since redirects are much more frequent than creates. Cache TTL is set to URL expiry time."

---

### Analytics

**Interviewer:** "How do you track clicks without slowing redirects?"

**You:** "Asynchronous processing. When a redirect happens, I immediately return the long URL and fire-and-forget add an event to a queue. A background thread processes events in batches every few seconds and updates the database. This keeps redirect latency under 10ms."

---

### Scalability

**Interviewer:** "How would you scale to billions of URLs?"

**You:**

**Database:**
- "Shard by short code hash for write distribution"
- "Read replicas for redirect traffic"
- "Index on short_code and long_url_hash"

**Cache:**
- "Redis Cluster with consistent hashing"
- "Cache warm-up for popular URLs"
- "TTL = URL expiry time"

**ID Generation:**
- "Twitter Snowflake for distributed IDs"
- "64-bit: timestamp + worker ID + sequence"
- "No coordination needed, 26K IDs/ms per worker"

**Analytics:**
- "Kafka instead of in-memory queue"
- "Separate analytics microservice"
- "Stream processing (Flink/Spark)"

**Global:**
- "CDN for edge caching"
- "Multi-region deployment"
- "GeoDNS routing"

---

## 📝 Quick Reference Card

```
┌───────────────────────────────────────┐
│ URL SHORTENER CHEAT SHEET             │
├───────────────────────────────────────┤
│ 1. Base62 Encoding:                   │
│    ID 12345 → "3D7"                   │
│    62^7 = 3.5 trillion                │
│                                        │
│ 2. Cache-First:                        │
│    Cache → DB → Cache                 │
│                                        │
│ 3. Async Analytics:                    │
│    Queue → Background → Batch Update  │
│                                        │
│ 4. Duplicate Detection:                │
│    SHA-256 hash → check → reuse       │
│                                        │
│ 5. Scalability:                        │
│    Shard DB, Redis Cluster, Snowflake │
└───────────────────────────────────────┘
```

---

## ⚠️ Common Mistakes to Avoid

### ❌ DON'T:

1. **Use MD5/SHA hash as short code**
   - Way too long
   - Use Base62!

2. **Query DB on every redirect**
   - Slow!
   - Use cache-first

3. **Update analytics synchronously**
   - Adds latency
   - Use async queue

4. **Generate random strings**
   - Collision risk
   - Harder to scale
   - Base62 is deterministic

5. **Ignore expiration**
   - Must return 410 Gone

---

### ✅ DO:

1. **Code Base62 encoder perfectly**
   - It's the signature algorithm
   - Interviewers expect this

2. **Explain cache-first clearly**
   - Shows performance awareness

3. **Mention async analytics**
   - Even if you don't code it

4. **Handle edge cases:**
   - Expired URLs (410)
   - Not found (404)
   - Duplicate URLs (reuse)

5. **Discuss sharding**
   - Shows scale awareness

---

## ✅ Pre-Interview Checklist

**Night before:**
- [ ] Read HLD.md (15 min)
- [ ] Read LLD.md (15 min)
- [ ] Read APPROACH.md (10 min)
- [ ] Run Main.java once

**30 min before:**
- [ ] Practice Base62 encoding on paper
- [ ] Review this INTERVIEW_GUIDE.md
- [ ] Memorize: 62^7 = 3.5 trillion

**Can you answer?**
- [ ] "How does Base62 work?" (explain + code)
- [ ] "How do you prevent duplicate URLs?" (hash)
- [ ] "How do you keep redirects fast?" (cache)
- [ ] "How would you scale this?" (shard, replicas, CDN)

---

## 🎯 SUCCESS = Show You Can:

1. ✅ **Code Base62 encoder** (signature algorithm)
2. ✅ **Implement cache-first** (shows performance thinking)
3. ✅ **Explain async analytics** (architecture awareness)
4. ✅ **Discuss scaling** (sharding, replication, distributed IDs)
5. ✅ **Handle edge cases** (expiration, duplicates, errors)

---

## 🚀 To Run:

```bash
cd lld-interview-problems
javac -d out -sourcepath src src/com/lld/urlshortener/Main.java
java -cp out com.lld.urlshortener.Main
```

---

## 💪 Final Tips

1. **Base62 is your friend**
   - Master it completely
   - It's the #1 thing interviewers test

2. **Cache-first always**
   - Mention it early
   - Shows you understand performance

3. **Async > Sync**
   - Analytics don't slow redirects
   - Shows good architecture

4. **Think distributed**
   - Snowflake for IDs
   - Sharding for scale
   - Shows big tech thinking

---

**YOU'RE READY!** 🎯

Code Base62, use cache, async analytics, discuss sharding.

That's it! 💪

Good luck! 🚀
