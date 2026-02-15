# Airbnb - Implementation Approach

## Interview Strategy: How to Approach This Problem

When you get "Design Airbnb" in an interview, follow this structured approach:

---

## Phase 1: Clarify Requirements (5-7 minutes)

### Questions to Ask Interviewer:

1. **Scope Clarification**
   - Are we building the entire platform or focusing on specific features?
   - Should we include payment processing, messaging, reviews?
   - Is this for LLD (classes/code) or HLD (system architecture)?

2. **User Types**
   - Are there different user roles (Host, Guest, Admin)?
   - Can a user be both host and guest?

3. **Property Features**
   - What property types? (Apartments, houses, rooms)
   - What amenities to support?
   - Photo uploads and management?

4. **Booking Flow**
   - Instant booking or host approval required?
   - How to handle double-booking prevention?
   - Cancellation policies?

5. **Search Requirements**
   - Search by location (city, coordinates, radius)?
   - Filter by dates, price, amenities?
   - How important is geolocation accuracy?

6. **Scale Expectations**
   - How many properties, users, bookings?
   - Concurrent search queries?
   - Read-heavy or write-heavy?

### Expected Answer from Interviewer:
"Focus on core features: user management, property listings, search by location/dates, booking with availability check, and basic payments. Use in-memory storage. Code the main classes and demonstrate with examples."

---

## Phase 2: High-Level Design (5-10 minutes)

### Step 1: Identify Core Entities
Start by listing main entities:
- **User** (Host, Guest)
- **Property** (Listing)
- **Booking** (Reservation)
- **Payment**
- **Review**
- **Location** (Address, lat/lng)

### Step 2: Define Key Features
Explain what each component does:
1. **User Management** - Register, login, profile
2. **Property Management** - Create listings, set availability
3. **Search & Discovery** - Find properties by location/dates
4. **Booking System** - Reserve property, check availability
5. **Payment** - Process payment, refunds
6. **Reviews** - Rate properties and users

### Step 3: Identify Relationships
- User → Properties (1:N for hosts)
- Property → Bookings (1:N)
- Booking → Payment (1:1)
- Property ↔ Amenities (N:N)

### Step 4: Discuss Key Challenges
1. **Geolocation Search** - Finding properties near a location
   - Solution: Calculate distance using Haversine formula
   - Filter by radius, then check availability

2. **Double-Booking Prevention** - Two users booking same dates
   - Solution: Check availability before confirming
   - Use synchronized blocks or database locks in production

3. **Availability Management** - Track blocked dates efficiently
   - Solution: Calendar with blocked date ranges
   - Check for overlapping date ranges

**Draw a simple diagram on whiteboard:**
```
User → Property → Calendar (Availability)
  ↓        ↓
Booking → Payment
  ↓
Review
```

---

## Phase 3: Low-Level Design (10-15 minutes)

### Step 1: Define Classes and Attributes

**Start with User:**
```java
class User {
    String userId;
    String name;
    String email;
    UserType type; // HOST, GUEST
    List<Property> hostedProperties;
    List<Booking> bookings;
}
```

**Then Property:**
```java
class Property {
    String propertyId;
    String hostId;
    String title;
    Location location;
    PropertyType type;
    double pricePerNight;
    PropertyCalendar calendar;
    List<Review> reviews;
}
```

**Continue with Booking, Payment, etc.**

### Step 2: Identify Enums
```java
enum UserType { HOST, GUEST }
enum PropertyType { APARTMENT, HOUSE, VILLA, ROOM }
enum BookingStatus { PENDING, CONFIRMED, CANCELLED, COMPLETED }
enum PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED }
```

### Step 3: Design Service Classes

Explain separation of concerns:
- **Models** = Data (User, Property, Booking)
- **Services** = Business Logic (BookingService, SearchService)

```java
class BookingService {
    Map<String, Booking> bookings;

    Booking createBooking(propertyId, guestId, checkIn, checkOut);
    void confirmBooking(bookingId);
    void cancelBooking(bookingId);
}
```

### Step 4: Apply Design Patterns

**Mention patterns you'll use:**
1. **Strategy Pattern** - Cancellation policies (Flexible, Moderate, Strict)
2. **Builder Pattern** - Creating complex Property objects
3. **Singleton** - Service classes
4. **Observer** - Notifications (optional)

**Example:**
```java
interface CancellationStrategy {
    double calculateRefund(Booking booking);
}

class FlexibleCancellation implements CancellationStrategy {
    public double calculateRefund(Booking booking) {
        // Full refund if >24 hours before check-in
    }
}
```

---

## Phase 4: Implementation (25-35 minutes)

### Step 1: Start with Models (10 min)

**Order of implementation:**
1. Enums first (quick, no dependencies)
2. Simple models (User, Location)
3. Complex models (Property with Calendar)
4. Booking and Payment

**Pro Tip:** Write minimal code first, add details later if time permits.

```java
// Start minimal
class Property {
    String propertyId;
    String title;
    Location location;
    double pricePerNight;

    // Add more fields only if time allows
}
```

### Step 2: Core Algorithm - Availability Check (5 min)

**This is critical - implement carefully:**
```java
public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
    for (Booking booking : existingBookings) {
        if (booking.getStatus() == CONFIRMED) {
            boolean overlaps = !(checkOut.isBefore(booking.getCheckInDate()) ||
                                checkIn.isAfter(booking.getCheckOutDate()));
            if (overlaps) return false;
        }
    }
    return true;
}
```

**Explain your logic out loud:**
"Two date ranges overlap unless one ends before the other starts."

### Step 3: Core Algorithm - Geolocation Search (5 min)

```java
public double calculateDistance(Location loc1, Location loc2) {
    // Haversine formula
    double lat1Rad = Math.toRadians(loc1.getLatitude());
    double lat2Rad = Math.toRadians(loc2.getLatitude());
    // ... full formula ...
    return distance;
}

public List<Property> searchByLocation(Location center, double radiusKm) {
    return properties.stream()
        .filter(p -> calculateDistance(center, p.getLocation()) <= radiusKm)
        .collect(Collectors.toList());
}
```

### Step 4: Service Classes (10 min)

Implement key services:
1. **PropertyService** - CRUD for properties
2. **BookingService** - Create, confirm, cancel bookings
3. **SearchService** - Search with filters

**Focus on 2-3 key methods per service.**

### Step 5: Main.java Demo (5 min)

Create a working demo:
```java
public static void main(String[] args) {
    // 1. Create users
    User host = new User("H1", "John Host", "john@example.com", HOST);
    User guest = new User("G1", "Jane Guest", "jane@example.com", GUEST);

    // 2. Create property
    Property property = new Property("P1", "H1", "Cozy Apartment",
                                    new Location("NYC", 40.7128, -74.0060),
                                    APARTMENT, 100.0);

    // 3. Search properties
    List<Property> results = searchService.searchByCity("NYC", checkIn, checkOut);

    // 4. Book property
    Booking booking = bookingService.createBooking("P1", "G1", checkIn, checkOut);

    // 5. Process payment
    Payment payment = paymentService.processPayment(booking.getId(), 300.0);

    // Print results
    System.out.println("Booking confirmed: " + booking.getId());
}
```

---

## Phase 5: Testing & Discussion (5 minutes)

### Walk Through Your Code
1. Explain the flow: "User searches → System filters by location and dates → User books → System checks availability → Processes payment"
2. Highlight design decisions: "I used Strategy pattern for cancellation policies because..."

### Discuss Edge Cases
- What if property not available? → Return error
- What if payment fails? → Cancel booking, notify user
- What if two users book simultaneously? → First check-then-set pattern, add locking in production

### Discuss Improvements
If interviewer asks "How would you improve this?"
- Add caching for popular searches (Redis)
- Use geospatial database (ElasticSearch, PostGIS)
- Implement proper locking for concurrent bookings (database transactions, distributed locks)
- Add pagination for search results
- Implement rate limiting for search API
- Add comprehensive error handling and logging

---

## Common Mistakes to Avoid

❌ **Don't:**
1. Jump straight to coding without clarifying requirements
2. Overcomplicate with too many features
3. Ignore availability check logic (critical!)
4. Forget to prevent double-booking
5. Hardcode everything without using enums
6. Create one giant "God class"
7. Forget to demonstrate your code with examples

✅ **Do:**
1. Clarify scope and requirements first
2. Start with high-level design, then dive into classes
3. Focus on core features (search, booking, availability)
4. Explain your thought process out loud
5. Use proper OOP principles and design patterns
6. Write clean, readable code with meaningful names
7. Test your code with a working Main.java

---

## Time Management (60-minute interview)

| Phase | Time | Activities |
|-------|------|------------|
| **Requirements** | 5-7 min | Ask questions, clarify scope |
| **HLD** | 5-10 min | Entities, relationships, key challenges |
| **LLD** | 10-15 min | Class design, attributes, methods |
| **Coding** | 25-35 min | Implement models, services, Main.java |
| **Testing** | 5 min | Walk through code, discuss trade-offs |

---

## Key Talking Points During Interview

1. **When designing User:**
   "I'm keeping UserType as enum because a user can be both host and guest. This is flexible and follows Open/Closed principle."

2. **When implementing availability check:**
   "The key challenge here is checking date overlaps. Two ranges overlap unless one ends before the other starts. This is O(n) where n is number of bookings. In production, we'd use a database index on dates."

3. **When designing search:**
   "For geolocation search, I'm using Haversine formula to calculate distance. In production, we'd use ElasticSearch with geo_point for sub-100ms queries. For now, I'll filter in-memory."

4. **When implementing booking:**
   "I'm checking availability before confirming. In production with concurrent requests, we'd use database transactions with SELECT FOR UPDATE or distributed locks like Redis to prevent race conditions."

5. **When discussing cancellation:**
   "I'm using Strategy pattern for cancellation policies. This makes it easy to add new policies (like 'Non-refundable') without changing existing code. This follows Open/Closed principle."

---

## What Interviewers Look For

✅ **Strong Candidates:**
- Clarify requirements before coding
- Design clean class hierarchy
- Implement working code
- Handle availability check correctly
- Apply design patterns appropriately
- Explain trade-offs and production considerations
- Write testable, maintainable code

❌ **Weak Candidates:**
- Jump straight to coding
- Create messy, tightly-coupled code
- Ignore edge cases (double-booking)
- Don't test their code
- Can't explain design decisions
- Overcomplicate or oversimplify

---

## Summary

**Your approach should be:**
1. ✅ Clarify → 2. ✅ Design (HLD + LLD) → 3. ✅ Implement → 4. ✅ Test → 5. ✅ Discuss

**Focus on:**
- Core features (don't overengineer)
- Clean OOP design
- Working code with demo
- Critical logic (availability, geolocation)
- Design patterns where appropriate

**Remember:**
- Think out loud - communication is key!
- It's okay to start simple and iterate
- Ask for feedback as you code
- Demonstrate your code works!

---

**Now proceed to the actual Java implementation in the following files!**
