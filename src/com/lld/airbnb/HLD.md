# Airbnb - High-Level Design (HLD)

## 1. Problem Statement

Design an online marketplace for short-term rentals where:
- **Hosts** can list their properties (homes, apartments, rooms)
- **Guests** can search, book, and pay for accommodations
- System handles availability, payments, reviews, and communication

---

## 2. Functional Requirements

### Core Features
1. **User Management**
   - Register as Host or Guest (or both)
   - Profile management (name, email, phone, verification)

2. **Property Management (Host)**
   - Add/Edit/Remove listings
   - Add property details (title, description, address, amenities)
   - Upload photos
   - Set pricing (per night, cleaning fee, service fee)
   - Manage availability calendar
   - Set house rules and cancellation policy

3. **Search & Discovery (Guest)**
   - Search by location (city, coordinates, address)
   - Filter by dates (check-in, check-out)
   - Filter by price range, property type, amenities, guests count
   - Sort by price, rating, popularity
   - View property details and photos

4. **Booking System**
   - Check availability for selected dates
   - Create booking request
   - Host can approve/reject booking
   - Instant booking option (auto-approve)
   - Block double-booking on same dates

5. **Payment System**
   - Calculate total cost (nights × price + fees)
   - Process payment (hold on booking, release after check-in)
   - Handle refunds based on cancellation policy
   - Payout to hosts

6. **Review & Rating System**
   - Guests review properties after stay
   - Hosts review guests
   - Calculate average ratings
   - Display reviews on listings

7. **Messaging System**
   - Host-Guest communication
   - Pre-booking inquiries
   - Post-booking coordination

8. **Cancellation**
   - Cancel booking based on policy
   - Process refunds accordingly

---

## 3. Non-Functional Requirements

### Performance
- **Low latency** for search queries (<200ms)
- Handle **high concurrent searches** (10K+ searches/sec)
- Efficient geolocation search

### Scalability
- Support millions of properties
- Handle peak booking seasons

### Availability
- 99.9% uptime
- No single point of failure

### Consistency
- **Strong consistency** for bookings (avoid double-booking)
- **Eventual consistency** for reviews, ratings

### Security
- Secure payment processing
- User data encryption
- Fraud detection

---

## 4. High-Level Components

### 4.1 Architecture Diagram (Text)

```
┌─────────────┐
│   Client    │ (Web/Mobile App)
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────────────────┐
│           API Gateway / Load Balancer           │
└──────────────────┬──────────────────────────────┘
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
┌──────────────┐        ┌──────────────┐
│ Auth Service │        │ Other APIs   │
└──────────────┘        └──────────────┘
                              │
       ┌──────────────────────┼──────────────────────┐
       ▼                      ▼                      ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   Search    │      │   Booking   │      │   Payment   │
│  Service    │      │   Service   │      │   Service   │
└──────┬──────┘      └──────┬──────┘      └──────┬──────┘
       │                    │                     │
       ▼                    ▼                     ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│ ElasticSearch│      │   Booking   │      │  Payment    │
│  / Geospatial│      │     DB      │      │  Gateway    │
│    Index     │      │             │      │             │
└─────────────┘      └─────────────┘      └─────────────┘

       ┌──────────────────────┐
       │  Messaging Service   │
       │   (WebSocket/Queue)  │
       └──────────────────────┘

       ┌──────────────────────┐
       │  Notification Service│
       │   (Email/SMS/Push)   │
       └──────────────────────┘
```

### 4.2 Core Services

#### **1. User Service**
- User registration/authentication
- Profile management
- User verification (email, phone, ID)

#### **2. Property Service**
- CRUD operations for listings
- Property details management
- Photo uploads (S3/CDN)
- Amenities management

#### **3. Search Service**
- **Geolocation search** (find properties within radius)
- Filter by dates, price, amenities, property type
- Ranking algorithm (by relevance, price, rating)
- **Caching layer** (Redis) for popular searches

#### **4. Availability Service**
- Manage property calendar
- Check date availability
- Block dates on booking
- Handle cancellations (unblock dates)

#### **5. Booking Service**
- Create booking requests
- Approval workflow (host approval or instant booking)
- **Prevent double-booking** (distributed locks/transactions)
- Booking state management

#### **6. Payment Service**
- Payment processing integration (Stripe, PayPal)
- Hold funds on booking
- Release to host after check-in
- Refund processing

#### **7. Review Service**
- Submit reviews and ratings
- Calculate aggregate ratings
- Display reviews with pagination

#### **8. Messaging Service**
- Real-time chat (WebSocket)
- Message persistence
- Notification on new messages

#### **9. Notification Service**
- Email notifications (booking confirmation, cancellation)
- SMS/Push notifications
- Template-based messaging

---

## 5. Database Design (High-Level)

### Relational Database (PostgreSQL/MySQL)
- **Users** table
- **Properties** table
- **Bookings** table
- **Payments** table
- **Reviews** table
- **Messages** table
- **Amenities** table
- **Property_Amenities** junction table

### NoSQL / Caching (Redis)
- **Search cache** (location + dates → property IDs)
- **Session storage**
- **Property view counts**

### Geospatial Database (ElasticSearch / PostGIS)
- Property locations indexed by lat/long
- Fast radius-based search

### Object Storage (S3)
- Property photos
- User profile pictures

---

## 6. Key APIs

### Search API
```
GET /api/v1/search?location={lat,lng}&radius={km}&checkIn={date}&checkOut={date}&guests={count}
Response: List of available properties with pricing
```

### Property APIs
```
POST   /api/v1/properties                 # Create listing
GET    /api/v1/properties/{id}            # Get details
PUT    /api/v1/properties/{id}            # Update
DELETE /api/v1/properties/{id}            # Remove
GET    /api/v1/properties/{id}/calendar   # Get availability
```

### Booking APIs
```
POST   /api/v1/bookings                   # Create booking
GET    /api/v1/bookings/{id}              # Get booking details
PUT    /api/v1/bookings/{id}/approve      # Host approves
PUT    /api/v1/bookings/{id}/cancel       # Cancel booking
```

### Payment APIs
```
POST   /api/v1/payments                   # Process payment
GET    /api/v1/payments/{id}              # Get payment status
POST   /api/v1/payments/{id}/refund       # Process refund
```

### Review APIs
```
POST   /api/v1/reviews                    # Submit review
GET    /api/v1/properties/{id}/reviews    # Get property reviews
```

---

## 7. Geolocation Search Strategy

### Challenge
Finding properties near a location for specific dates efficiently.

### Solution
1. **Geospatial Index** (ElasticSearch with geo_point or PostGIS)
   - Index properties by (latitude, longitude)
   - Use geo-distance query to find properties within radius

2. **Two-step Search**
   - Step 1: Find properties by location (geospatial query)
   - Step 2: Filter by date availability (check booking calendar)

3. **Caching**
   - Cache popular city searches in Redis
   - Cache key: `search:{city}:{checkIn}:{checkOut}:{filters}`
   - TTL: 5-10 minutes

### Example Query Flow
```
1. User searches: "San Francisco, CA, Dec 25-30, 2 guests"
2. Convert city → lat/lng (37.7749, -122.4194)
3. ElasticSearch: Find properties within 25km radius
4. Filter: Check availability for Dec 25-30
5. Rank: Sort by relevance (price, rating, availability)
6. Return: Top N results with pricing
```

---

## 8. Avoiding Double-Booking

### Problem
Two users trying to book the same property for overlapping dates simultaneously.

### Solution
1. **Optimistic Locking**
   - Add `version` column to booking table
   - Check version before committing booking
   - Retry on conflict

2. **Pessimistic Locking**
   - Use `SELECT FOR UPDATE` when checking availability
   - Lock calendar rows during booking transaction

3. **Distributed Locks** (Redis)
   - Acquire lock: `LOCK:property:{id}:dates:{checkIn}-{checkOut}`
   - Hold lock during booking creation
   - Release after commit/rollback

### Booking Flow
```
1. User selects dates and clicks "Book"
2. Acquire lock on property + dates
3. Check availability (no overlapping bookings)
4. Create booking record
5. Block dates in calendar
6. Process payment
7. Release lock
8. Send confirmation
```

---

## 9. Technology Stack (Suggested)

- **Backend:** Java (Spring Boot) / Python (Django) / Node.js
- **Database:** PostgreSQL (primary), Redis (cache), ElasticSearch (search)
- **Message Queue:** RabbitMQ / Kafka (async processing)
- **Storage:** AWS S3 (photos)
- **CDN:** CloudFront (photo delivery)
- **Payment:** Stripe API / PayPal
- **WebSocket:** Socket.io / AWS API Gateway WebSocket
- **Deployment:** Docker, Kubernetes, AWS/GCP

---

## 10. Scalability Considerations

### Horizontal Scaling
- Load balance across multiple service instances
- Shard database by region or property ID

### Caching Strategy
- Cache search results (Redis)
- Cache property details (reduce DB load)
- CDN for static assets (photos)

### Asynchronous Processing
- Use message queues for:
  - Sending notifications
  - Processing payments
  - Updating search index
  - Generating reports

### Database Optimization
- Index frequently queried columns (location, dates, status)
- Partition bookings table by date range
- Read replicas for search queries

---

## 11. Failure Handling

- **Payment Failure:** Retry + notify user
- **Booking Conflict:** Show error, suggest alternative dates
- **Service Downtime:** Circuit breaker pattern, fallback responses
- **Data Inconsistency:** Event sourcing, audit logs

---

## Summary

Airbnb's design focuses on:
1. **Efficient geolocation search** (ElasticSearch + caching)
2. **Strong consistency for bookings** (avoid double-booking)
3. **Scalable architecture** (microservices, horizontal scaling)
4. **Rich search experience** (filters, ranking, availability)
5. **Secure payment processing** (hold/release funds)

Next: See **LLD.md** for detailed class design and **APPROACH.md** for implementation strategy.
