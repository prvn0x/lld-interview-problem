# Airbnb - Low-Level Design Implementation

## 📝 Problem Statement

Design an online marketplace for short-term rentals (similar to Airbnb) where:
- Hosts can list properties with details, pricing, and availability
- Guests can search properties by location/dates and make bookings
- System handles payments, reviews, and host-guest communication
- Prevents double-booking and manages cancellation policies

---

## 🎯 Features Implemented

### ✅ Core Features
1. **User Management**
   - Register as Host, Guest, or Both
   - User verification
   - Profile with ratings

2. **Property Management**
   - Create/Edit/Delete property listings
   - Set amenities, photos, pricing
   - Configure cancellation policy
   - Instant booking vs manual approval
   - Availability calendar

3. **Search & Discovery**
   - Search by city name
   - Search by location radius (geolocation)
   - Filter by price, property type, amenities, bedrooms
   - Sort by price, rating, popularity

4. **Booking System**
   - Check availability before booking
   - Prevent double-booking (date conflict check)
   - Calculate total cost (nights + cleaning + service fees)
   - Instant booking or host approval
   - Booking status management

5. **Payment Processing**
   - Process payments with dummy data
   - Hold and release funds
   - Refund based on cancellation policy

6. **Review & Rating System**
   - Guests review properties
   - Hosts review guests
   - Aggregate ratings calculation

7. **Messaging**
   - Host-guest communication
   - Conversation history
   - Unread message tracking

8. **Cancellation & Refunds**
   - Three policies: Flexible, Moderate, Strict
   - Automatic refund calculation based on policy

---

## 🏗️ Architecture

### Package Structure
```
com.lld.airbnb/
├── enums/                      # UserType, PropertyType, BookingStatus, etc.
├── models/                     # Data models (User, Property, Booking, etc.)
├── services/                   # Business logic services
└── Main.java                   # Demo application
```

### Design Patterns Used
- **Strategy Pattern**: Cancellation policies (Flexible, Moderate, Strict)
- **Singleton Pattern**: Service classes
- **Builder Pattern**: Complex object construction (can be extended)

### Key Algorithms
1. **Haversine Formula**: Calculate distance between two lat/lng coordinates
2. **Date Overlap Detection**: Check if two date ranges conflict
3. **Search with Filters**: Multi-criteria filtering and sorting

---

## 🚀 How to Run

### Using IntelliJ IDEA (Recommended)
1. Open project in IntelliJ
2. Navigate to `src/com/lld/airbnb/Main.java`
3. Right-click → **Run 'Main.main()'**
4. See output in console!

### Using Command Line
```bash
# From project root directory
cd /Users/praveen.singh/Desktop/lld-interview-problems

# Compile
javac -d out -sourcepath src src/com/lld/airbnb/Main.java

# Run
java -cp out com.lld.airbnb.Main
```

---

## 📊 Demo Flow

The `Main.java` demonstrates:

1. **Register Users**: 2 hosts + 2 guests
2. **Create Properties**: 4 properties (NYC, SF, LA) with amenities
3. **Search**: By city, by radius, with filters
4. **View Details**: Property info, cost calculation
5. **Book**: Create booking with availability check
6. **Pay**: Process payment
7. **Message**: Host-guest communication
8. **Review**: After stay completion
9. **Cancel**: With refund based on policy
10. **Dashboards**: Host and guest views

---

## 🧮 Key Logic Explained

### 1. Availability Check (Prevent Double-Booking)
```java
public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
    for (DateRange blocked : blockedDateRanges) {
        if (requestedRange.overlaps(blocked)) {
            return false;  // Conflict found
        }
    }
    return true;  // Available
}
```

**Logic**: Two date ranges overlap unless one ends before the other starts.

### 2. Geolocation Search (Haversine Formula)
```java
public double calculateDistance(Location loc1, Location loc2) {
    // Earth radius in km
    final int R = 6371;

    // Convert to radians and calculate
    double a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlng/2);
    double c = 2 * atan2(√a, √(1-a));

    return R * c;  // Distance in km
}
```

### 3. Refund Calculation (Strategy Pattern)
```java
switch (cancellationPolicy) {
    case FLEXIBLE:
        return (daysUntilCheckIn >= 1) ? fullRefund : 50% refund;
    case MODERATE:
        return (daysUntilCheckIn >= 5) ? fullRefund :
               (daysUntilCheckIn >= 1) ? 50% : noRefund;
    case STRICT:
        return (daysUntilCheckIn >= 7) ? 50% : noRefund;
}
```

---

## 🗂️ Data Models

### User
- userId, name, email, phone, userType (HOST/GUEST)
- hostedProperties, bookings
- avgRating (as host and guest)

### Property
- propertyId, hostId, title, description, type
- location (lat/lng), maxGuests, bedrooms, bathrooms
- amenities, photos, pricing
- calendar (availability), reviews, ratings

### Booking
- bookingId, propertyId, guestId, hostId
- checkIn, checkOut, guests, nights
- totalAmount, status, paymentId

### Payment
- paymentId, bookingId, amount, status
- paymentMethod, transactionId
- refundAmount, refundDate

### Review
- reviewId, bookingId, reviewerId, revieweeId
- propertyId, rating (1-5), comment

---

## 💾 In-Memory Storage

All data is stored in HashMaps (no database):
```java
Map<String, User> users;
Map<String, Property> properties;
Map<String, Booking> bookings;
Map<String, Payment> payments;
Map<String, Review> reviews;
Map<String, Message> messages;
```

---

## 🎯 Interview Discussion Points

### Scalability Improvements
1. **Search Optimization**
   - Use ElasticSearch with geospatial indexing
   - Cache popular city searches in Redis
   - CDN for property photos

2. **Concurrency Handling**
   - Distributed locks (Redis) for booking
   - Database transactions with SELECT FOR UPDATE
   - Event-driven architecture for notifications

3. **Database Sharding**
   - Shard by property location (region-based)
   - Separate read replicas for search queries

### Trade-offs
- **In-memory vs Database**: Fast but not persistent
- **Blocking dates vs Booking records**: Simple but less flexible
- **Synchronous vs Async**: Easier to implement but less scalable

### Production Considerations
- Rate limiting on search API
- Payment gateway integration (Stripe, PayPal)
- Real-time messaging (WebSocket, Firebase)
- Photo storage (AWS S3, CDN)
- Email/SMS notifications
- Analytics and logging
- Fraud detection
- Multi-currency support

---

## 📚 Learning Resources

- [Haversine Formula Explanation](https://en.wikipedia.org/wiki/Haversine_formula)
- [Date Overlap Algorithm](https://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap)
- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy)

---

## ✅ Checklist for Interview

- [x] Clarified requirements (scope, features, scale)
- [x] Designed core entities (User, Property, Booking)
- [x] Implemented availability check (critical!)
- [x] Implemented geolocation search
- [x] Applied design patterns (Strategy)
- [x] Handled edge cases (double-booking, cancellation)
- [x] Created working demo with dummy data
- [x] Discussed scalability and trade-offs

---

## 🎓 Time Complexity Analysis

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Search by city | O(n) | O(1) |
| Search by radius | O(n) | O(1) |
| Check availability | O(m) | O(1) |
| Book property | O(m) | O(1) |
| Filter/Sort | O(n log n) | O(n) |

Where:
- n = number of properties
- m = number of existing bookings for a property

---

**Author**: Praveen Singh
**Date**: February 2026
**Purpose**: LLD Interview Preparation
