# 🎯 AIRBNB - INTERVIEW GUIDE

## What You Need to Memorize & Show

---

## 📂 Final Clean Structure (Interview-Ready)

```
airbnb/
├── HLD.md              # Read before interview - High-level design
├── LLD.md              # Read before interview - Class design
├── APPROACH.md         # Read before interview - How to approach
├── README.md           # Quick reference during interview
│
├── enums/              # 6 enums - memorize these
├── models/             # 8 models - know the key ones
├── services/           # 7 services - understand the flow
├── strategies/         # 5 files - Strategy Pattern (IMPORTANT!)
│
└── Main.java          # ✅ RUN THIS IN INTERVIEW
```

---

## 🧠 What to MEMORIZE (5-10 minutes before interview)

### 1. Core Entities (4 main ones)
```
User → Property → Booking → Payment
  ↓        ↓         ↓
Reviews  Amenities  Refunds
```

### 2. Key Algorithm #1: Availability Check
```java
// Prevent double-booking
boolean overlaps = !(checkOut.isBefore(existingCheckIn) ||
                     checkIn.isAfter(existingCheckOut));
if (overlaps) return false; // NOT available
```
**Say:** "Two date ranges overlap unless one ends before the other starts"

### 3. Key Algorithm #2: Geolocation Search
```java
// Haversine formula - calculate distance between two points
distance = 2 * R * atan2(√a, √(1-a))
```
**Say:** "Uses Haversine formula to find properties within radius. In production, we'd use ElasticSearch with geospatial indexing"

### 4. Design Pattern: Strategy Pattern
```java
interface CancellationStrategy {
    double calculateRefund(Booking, daysUntilCheckIn);
}

// Usage
CancellationStrategy strategy = factory.getStrategy(policy);
return strategy.calculateRefund(booking, days);
```
**Say:** "Strategy Pattern for cancellation policies. Follows Open/Closed Principle - can add new policies without modifying PaymentService"

---

## 🎬 Interview Flow (Step-by-Step)

### PHASE 1: Clarify Requirements (5 min)
**Ask:**
- "Should I focus on core booking flow or also include reviews/messaging?"
- "Do we need to handle concurrent bookings? How to prevent double-booking?"
- "In-memory storage or discuss database design?"
- "Search by location - how accurate? City-level or GPS coordinates?"

**Expected answer:** "Focus on core: search, booking, payment. Use in-memory. Prevent double-booking."

---

### PHASE 2: High-Level Design (10 min)

**Draw on whiteboard:**
```
┌─────────┐
│  User   │ (Host / Guest)
└────┬────┘
     │
     ▼
┌──────────┐      ┌──────────┐
│ Property │──────│ Calendar │ (Availability)
└────┬─────┘      └──────────┘
     │
     ▼
┌──────────┐      ┌──────────┐
│ Booking  │──────│ Payment  │
└────┬─────┘      └──────────┘
     │
     ▼
┌──────────┐
│  Review  │
└──────────┘
```

**Mention:**
- "User can be both Host and Guest"
- "Property has Calendar to track availability"
- "Booking connects Property, Guest, and Payment"
- "Reviews go both ways: Guest→Property, Host→Guest"

**Key challenges:**
1. Geolocation search → "Haversine formula, in prod use ElasticSearch"
2. Double-booking → "Check date overlaps before confirming"
3. Cancellation policies → "Strategy Pattern for flexibility"

---

### PHASE 3: Class Design (15 min)

**Start with main classes:**

```java
// 1. User
class User {
    String userId, name, email;
    UserType type; // HOST, GUEST, BOTH
    List<String> propertyIds;
    List<String> bookingIds;
}

// 2. Property
class Property {
    String propertyId, hostId, title;
    Location location; // lat, lng
    PropertyType type; // APARTMENT, HOUSE
    double pricePerNight;
    PropertyCalendar calendar; // Availability
    List<Amenity> amenities;
}

// 3. Booking
class Booking {
    String bookingId, propertyId, guestId;
    LocalDate checkIn, checkOut;
    BookingStatus status; // PENDING, CONFIRMED
    double totalAmount;
}

// 4. Payment
class Payment {
    String paymentId, bookingId;
    double amount;
    PaymentStatus status;
}
```

**Mention:**
- "PropertyCalendar manages blocked dates"
- "Location has lat/lng for geosearch"
- "Using enums for status and types"

---

### PHASE 4: Implementation (30 min)

**Order to implement:**
1. ✅ Enums (2 min) - quick wins
2. ✅ Location + User (5 min) - simple classes
3. ✅ PropertyCalendar (5 min) - **critical for availability**
4. ✅ Property (5 min)
5. ✅ Booking + Payment (5 min)
6. ✅ Services (8 min) - PropertyService, BookingService, SearchService
7. ✅ **Strategy Pattern** (if time) (5 min) - cancellation policies
8. ✅ Main.java demo (5 min)

**Don't get stuck on:**
- ❌ Perfect getter/setters
- ❌ All amenities
- ❌ Full messaging system
- ❌ Complete review logic

**Focus on:**
- ✅ Availability check (critical!)
- ✅ Search by location
- ✅ Booking flow
- ✅ One design pattern (Strategy)

---

### PHASE 5: Demo (5 min)

**Run Main.java:**
```bash
javac -d out -sourcepath src src/com/lld/airbnb/Main.java
java -cp out com.lld.airbnb.Main
```

**Walk through output:**
1. "Here we register users - 2 hosts, 2 guests"
2. "Created 4 properties in different cities with amenities"
3. "Search by city - finds 2 properties in NYC"
4. "Geolocation search within 10km radius"
5. "Booking with availability check - prevents double-booking"
6. "Payment processed"
7. "Cancellation with refund - using Strategy Pattern"

**Point out:**
- ✅ "No double-booking - date overlap check works"
- ✅ "Geolocation search using Haversine formula"
- ✅ "Strategy Pattern for cancellation policies"

---

## 🎯 Key Points to Mention

### 1. When Discussing Availability:
**You:** "The key is checking date overlap. Two ranges overlap unless one ends before the other starts. I'm using a Set of DateRange objects. In production, we'd use database transactions with SELECT FOR UPDATE to handle concurrent bookings."

### 2. When Discussing Search:
**You:** "I'm using Haversine formula to calculate distance between coordinates. This is O(n) but works for demo. In production, we'd use ElasticSearch with geo_point indexing for sub-100ms queries, and cache popular city searches in Redis."

### 3. When Discussing Strategy Pattern:
**You:** "I implemented Strategy Pattern for cancellation policies. Each policy (Flexible, Moderate, Strict) is a separate class. This follows Open/Closed Principle - I can add a new 'Non-Refundable' policy by just creating one class, no need to modify PaymentService."

**[Show them the strategies/ folder]**

### 4. When Discussing Scalability:
**You:** "For scale, we'd:
- Use ElasticSearch for geospatial search
- Redis for caching popular searches
- Database sharding by region
- Message queue for async notifications
- CDN for property photos on S3"

---

## 🚀 Practice Run (Do This Before Interview!)

**Timing yourself:**

1. **Setup** (1 min)
   ```bash
   cd lld-interview-problems
   ```

2. **Explain approach** (2 min)
   - "I'll design User, Property, Booking, Payment"
   - "Key challenges: availability check, geosearch"
   - "Will use Strategy Pattern for cancellation"

3. **Code the main classes** (20 min)
   - Start with enums
   - Then models
   - Then services
   - Don't code everything perfectly!

4. **Run demo** (2 min)
   ```bash
   javac -d out -sourcepath src src/com/lld/airbnb/Main.java
   java -cp out com.lld.airbnb.Main
   ```

5. **Walk through** (5 min)
   - Show search working
   - Show booking preventing double-booking
   - Show Strategy Pattern for cancellation

**Total: 30 minutes** ✅

---

## 📝 Quick Reference Card

Keep this handy during interview:

```
┌─────────────────────────────────────────────┐
│ AIRBNB CHEAT SHEET                          │
├─────────────────────────────────────────────┤
│ 1. Core Entities:                           │
│    User → Property → Booking → Payment      │
│                                              │
│ 2. Key Algorithm:                            │
│    overlaps = !(end1 < start2 || start1 > end2) │
│                                              │
│ 3. Design Pattern:                           │
│    Strategy Pattern for cancellation         │
│                                              │
│ 4. Geolocation:                              │
│    Haversine formula, ElasticSearch in prod  │
│                                              │
│ 5. Scalability:                              │
│    Redis cache, DB sharding, S3+CDN          │
└─────────────────────────────────────────────┘
```

---

## ✅ Pre-Interview Checklist

Night before:
- [ ] Read HLD.md (10 min)
- [ ] Read LLD.md (15 min)
- [ ] Read APPROACH.md (10 min)
- [ ] Run Main.java once to see output

30 min before:
- [ ] Review this INTERVIEW_GUIDE.md
- [ ] Memorize: date overlap check
- [ ] Memorize: Strategy Pattern explanation
- [ ] Practice: "Two date ranges overlap unless..."

---

## 🎯 SUCCESS = Show You Can:

1. ✅ **Design** clean class hierarchy
2. ✅ **Implement** critical algorithm (availability check)
3. ✅ **Apply** design pattern (Strategy)
4. ✅ **Explain** scalability considerations
5. ✅ **Demo** working code

**Not about:**
- ❌ Perfect code
- ❌ Every feature
- ❌ All design patterns
- ❌ Production-ready error handling

**About:**
- ✅ Can you solve the core problem?
- ✅ Can you design extensible code?
- ✅ Can you think about scale?
- ✅ Can you write working code?

---

## 🔥 Final Tip

**Interviewer:** "How would you improve this?"

**You:** "Three main areas:
1. **Search:** ElasticSearch with geospatial indexing + Redis caching
2. **Concurrency:** Distributed locks (Redis) or DB transactions for bookings
3. **Scalability:** Microservices, database sharding by region, CDN for photos"

**Don't say:**
- ❌ "I'd rewrite everything"
- ❌ "This code is bad"
- ❌ "I need more time"

---

**YOU'RE READY!** 🚀

Just run: `java -cp out com.lld.airbnb.Main`

And explain your design!

Good luck! 💪
