# URL Shortener - High-Level Design (HLD)

## 1. Problem Statement

Design a URL shortening service (like bit.ly, TinyURL) that:
- Converts long URLs into short, easy-to-share URLs
- Redirects short URLs to original long URLs
- Tracks analytics (click counts)
- Supports URL expiration
- Handles high read traffic efficiently

---

## 2. Functional Requirements

### Core Features

1. **Create Short URL**
   - Accept long URL
   - Generate unique short code (6-8 characters)
   - Optional custom expiration time
   - Return shortened URL
   - Idempotent: same long URL → same short URL

2. **Redirect to Original URL**
   - Input: short code
   - Return: HTTP 302 redirect to long URL
   - Handle expired URLs (HTTP 410)
   - Handle not found (HTTP 404)

3. **URL Expiration**
   - Default expiry: 1 year
   - Custom expiry allowed
   - Expired URLs must not redirect

4. **Basic Analytics**
   - Track total clicks per short URL
   - Analytics update asynchronously (don't slow redirects)

5. **Authentication**
   - API key required for URL creation
   - Public access for redirects (no auth)

---

## 3. Non-Functional Requirements

### Performance
- **Read-heavy workload**: 100:1 read-to-write ratio
- **Low latency redirects**: < 10ms p99
- **High throughput**: 10K+ redirects/sec

### Scalability
- Support **billions of URLs**
- Horizontal scaling
- No single point of failure

### Availability
- **99.9% uptime** for redirect service
- Graceful degradation

### Security
- Short codes should be **hard to guess** (not sequential)
- Rate limiting on creation
- Prevent malicious URLs

---

## 4. High-Level Architecture

### 4.1 System Components

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│      Load Balancer / CDN        │
└──────────────┬──────────────────┘
               │
       ┌───────┴────────┐
       ▼                ▼
┌─────────────┐   ┌─────────────┐
│  API Server │   │  API Server │  (Stateless)
│  (Create)   │   │  (Redirect) │
└──────┬──────┘   └──────┬──────┘
       │                  │
       │         ┌────────┴─────────┐
       │         ▼                  ▼
       │   ┌──────────┐      ┌──────────┐
       │   │  Cache   │      │ Database │
       │   │ (Redis)  │      │ (Primary)│
       │   └──────────┘      └──────────┘
       │                           │
       ▼                           │
┌──────────────┐                  │
│  Analytics   │◄─────────────────┘
│    Queue     │
└──────────────┘
```

### 4.2 Service Breakdown

#### **1. URL Shortening Service**
- Generate unique short codes
- Store URL mappings
- Return shortened URL

#### **2. Redirect Service**
- Lookup short code (cache-first)
- Validate expiration
- Return 302 redirect
- Update analytics asynchronously

#### **3. Analytics Service**
- Track click counts
- Process events from queue
- Batch updates to database

#### **4. Cache Layer**
- In-memory cache (Redis)
- Cache-aside pattern
- TTL = URL expiry time

---

## 5. Database Design (High-Level)

### Primary Table: `url_mappings`

```sql
CREATE TABLE url_mappings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) UNIQUE NOT NULL,
    long_url TEXT NOT NULL,
    long_url_hash VARCHAR(64) NOT NULL,  -- For duplicate detection
    created_at TIMESTAMP DEFAULT NOW(),
    expiry_time TIMESTAMP,
    status ENUM('ACTIVE', 'EXPIRED') DEFAULT 'ACTIVE',
    click_count BIGINT DEFAULT 0,
    created_by VARCHAR(50),  -- API key or user ID

    INDEX idx_short_code (short_code),
    INDEX idx_long_url_hash (long_url_hash),
    INDEX idx_expiry_time (expiry_time)
);
```

### Secondary Table: `analytics_events` (Optional)

```sql
CREATE TABLE analytics_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    user_agent TEXT,
    ip_address VARCHAR(45),

    INDEX idx_short_code_timestamp (short_code, timestamp)
);
```

---

## 6. API Design

### Create Short URL

```http
POST /api/v1/shorten
Headers:
  Content-Type: application/json
  API-Key: <api_key>

Request:
{
  "longUrl": "https://example.com/very/long/url",
  "expiryTime": "2027-01-01T00:00:00Z"  // optional, default = 1 year
}

Response (201 Created):
{
  "shortUrl": "https://short.ly/abc123",
  "shortCode": "abc123",
  "longUrl": "https://example.com/very/long/url",
  "expiryTime": "2027-01-01T00:00:00Z",
  "createdAt": "2026-02-15T10:00:00Z"
}
```

### Redirect

```http
GET /{shortCode}

Responses:
- 302 Found → Location: <long_url>
- 404 Not Found → Short code doesn't exist
- 410 Gone → URL has expired
- 429 Too Many Requests → Rate limited
```

### Get Analytics

```http
GET /api/v1/analytics/{shortCode}
Headers:
  API-Key: <api_key>

Response:
{
  "shortCode": "abc123",
  "longUrl": "https://example.com/...",
  "clickCount": 10234,
  "createdAt": "2026-02-15T10:00:00Z",
  "expiryTime": "2027-01-01T00:00:00Z"
}
```

---

## 7. Short Code Generation Strategy

### Approach: Base62 Encoding

**Why Base62?**
- Uses: `[A-Z, a-z, 0-9]` = 62 characters
- URL-safe (no special chars)
- Compact representation

**Process:**
1. Generate unique numeric ID (auto-increment or distributed ID generator)
2. Encode ID using Base62
3. Result: 6-7 character short code

**Example:**
```
ID = 123456789
Base62(123456789) = "8M0kX"
Short URL = https://short.ly/8M0kX
```

**Collision Handling:**
- Auto-increment IDs → No collisions
- Check uniqueness before inserting

**Length Calculation:**
```
62^6 = 56 billion unique URLs
62^7 = 3.5 trillion unique URLs
```

### Alternative: Random String Generation
- Generate random 7-char string
- Check for collision in DB
- Retry if collision (rare)

---

## 8. Caching Strategy

### Cache-Aside Pattern

**On Read (Redirect):**
```
1. Check cache for shortCode
2. If HIT → return longUrl
3. If MISS → query DB
4. Store in cache (TTL = expiry time)
5. Return longUrl
```

**Cache Key:** `url:{shortCode}`

**Cache Value:**
```json
{
  "longUrl": "https://...",
  "expiryTime": "2027-01-01T00:00:00Z"
}
```

**TTL:** Set to URL expiry time

**Cache Invalidation:**
- Automatic (TTL-based)
- Manual if URL deleted

---

## 9. Analytics Strategy

### Problem
- Updating DB on every redirect is slow
- Would add 10-20ms latency

### Solution: Asynchronous Updates

**Flow:**
```
1. Redirect request arrives
2. Return redirect immediately (< 10ms)
3. Fire-and-forget: send event to queue
4. Background worker processes queue
5. Batch update click counts every N seconds
```

**Queue:**
- In-memory queue (Java: BlockingQueue)
- Message queue (Kafka, RabbitMQ) in production

**Batch Update:**
```sql
UPDATE url_mappings
SET click_count = click_count + ?
WHERE short_code = ?
```

---

## 10. Preventing Duplicate URLs

### Problem
Same long URL submitted multiple times → generate multiple short codes?

### Solution: Hash-Based Deduplication

**Process:**
1. Hash long URL (SHA-256)
2. Store hash in `long_url_hash` column
3. Before creating new short URL:
   - Check if hash exists
   - If exists → return existing short code
   - If not → create new

**SQL:**
```sql
SELECT short_code FROM url_mappings
WHERE long_url_hash = ?
AND status = 'ACTIVE'
AND (expiry_time IS NULL OR expiry_time > NOW());
```

---

## 11. Handling Expiration

### Background Job

**Every 1 hour:**
```sql
UPDATE url_mappings
SET status = 'EXPIRED'
WHERE expiry_time < NOW()
AND status = 'ACTIVE';
```

### On Redirect

**Check expiry:**
```java
if (urlMapping.getExpiryTime() != null &&
    urlMapping.getExpiryTime().isBefore(LocalDateTime.now())) {
    return HTTP 410 Gone;
}
```

---

## 12. Scalability Considerations

### Horizontal Scaling

**API Servers:**
- Stateless design
- Scale behind load balancer
- No session state

**Database:**
- Read replicas for redirects
- Master for writes
- Sharding by short code hash

**Cache:**
- Distributed cache (Redis Cluster)
- Consistent hashing

### Distributed ID Generation

**Options:**

1. **Database Auto-Increment**
   - Simple but single point of failure
   - Use with read replicas

2. **Twitter Snowflake**
   - 64-bit ID: timestamp + worker ID + sequence
   - Decentralized, high throughput

3. **UUID**
   - Longer codes (not ideal)
   - No coordination needed

**Recommended:** Snowflake for distributed system

---

## 13. Rate Limiting

### Protection Against Abuse

**Per API Key:**
- 100 URL creations per hour
- 10,000 redirects per hour

**Implementation:**
- Token bucket algorithm
- Redis for distributed rate limiting

**Key:** `ratelimit:{apiKey}:{hour}`

---

## 14. Security Considerations

### URL Validation
- Check for malicious URLs
- Blacklist known phishing domains
- Virus scan (optional, via API)

### Short Code Guessing
- Use Base62 (not sequential)
- Makes brute force hard

### DDoS Protection
- CDN (CloudFlare, Akamai)
- Rate limiting
- WAF (Web Application Firewall)

---

## 15. Technology Stack (Suggested)

**Backend:**
- Java (Spring Boot) / Node.js / Python (FastAPI)

**Database:**
- PostgreSQL / MySQL (relational)
- Cassandra / DynamoDB (NoSQL, for massive scale)

**Cache:**
- Redis / Memcached

**Message Queue:**
- Kafka / RabbitMQ / AWS SQS

**Load Balancer:**
- Nginx / HAProxy / AWS ALB

**Monitoring:**
- Prometheus + Grafana
- ELK Stack (Elasticsearch, Logstash, Kibana)

---

## 16. Traffic Estimation

### Assumptions
- 100M URL creations per month
- 100:1 read-to-write ratio
- 10B redirects per month

### QPS Calculation
```
Writes:
  100M / (30 days × 86400 sec) = ~40 writes/sec
  Peak = 400 writes/sec (10x)

Reads:
  10B / (30 days × 86400 sec) = ~4000 reads/sec
  Peak = 40K reads/sec (10x)
```

### Storage
```
URL size: ~500 bytes (URL + metadata)
100M URLs/month × 500 bytes = 50 GB/month
1 year = 600 GB
5 years = 3 TB
```

---

## 17. Failure Scenarios & Handling

### Database Down
- Cache continues serving redirects (stale data okay)
- Queue new creations, process when DB recovers

### Cache Down
- Fall back to database (higher latency)
- Auto-scale DB read replicas

### Analytics Queue Full
- Drop analytics events (non-critical)
- Alert operations team

---

## Summary

**Key Design Decisions:**

1. **Base62 encoding** for short codes
2. **Cache-first** architecture for low-latency redirects
3. **Asynchronous analytics** to avoid slowing redirects
4. **Hash-based deduplication** to prevent duplicate URLs
5. **Horizontal scaling** with stateless services
6. **Distributed ID generation** for uniqueness at scale

**Next:** See **LLD.md** for detailed class design and **APPROACH.md** for implementation strategy.
