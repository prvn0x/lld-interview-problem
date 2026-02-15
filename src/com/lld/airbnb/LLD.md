# Airbnb - Low-Level Design (LLD)

## 1. Class Diagram Overview

### Core Entities (Models)
- User
- Property
- Booking
- Payment
- Review
- Message
- Location
- Calendar
- Amenity

### Services (Business Logic)
- UserService
- PropertyService
- SearchService
- BookingService
- PaymentService
- ReviewService
- MessagingService

### Enums
- UserType (HOST, GUEST)
- PropertyType (APARTMENT, HOUSE, VILLA, ROOM)
- BookingStatus (PENDING, CONFIRMED, CANCELLED, COMPLETED)
- PaymentStatus (PENDING, SUCCESS, FAILED, REFUNDED)
- CancellationPolicy (FLEXIBLE, MODERATE, STRICT)

### Design Patterns Used
- **Strategy Pattern:** Different pricing strategies, cancellation policies
- **Factory Pattern:** Creating different property types
- **Observer Pattern:** Notifications on booking events
- **Singleton Pattern:** Service classes
- **Builder Pattern:** Complex object creation (Property, Booking)

---

## 2. Detailed Class Design

### 2.1 User Class

```java
class User {
    - String userId              // Unique ID
    - String name
    - String email
    - String phone
    - UserType userType          // HOST, GUEST, BOTH
    - boolean isVerified
    - LocalDateTime createdAt
    - List<Property> hostedProperties    // If host
    - List<Booking> bookings             // If guest
    - double avgRatingAsHost
    - double avgRatingAsGuest

    + User(userId, name, email, phone, userType)
    + getters/setters
}
```

### 2.2 Location Class

```java
class Location {
    - String address
    - String city
    - String state
    - String country
    - String zipCode
    - double latitude
    - double longitude

    + Location(address, city, state, country, lat, lng)
    + double calculateDistance(Location other)  // Haversine formula
    + getters/setters
}
```

### 2.3 Property Class

```java
class Property {
    - String propertyId
    - String hostId              // Reference to User
    - String title
    - String description
    - PropertyType type          // APARTMENT, HOUSE, etc.
    - Location location
    - int maxGuests
    - int bedrooms
    - int bathrooms
    - List<Amenity> amenities    // WiFi, Kitchen, Parking, etc.
    - List<String> photoUrls
    - double pricePerNight
    - double cleaningFee
    - double serviceFee
    - CancellationPolicy cancellationPolicy
    - PropertyCalendar calendar  // Availability
    - List<Review> reviews
    - double avgRating
    - int totalBookings
    - boolean instantBooking     // Auto-approve or manual
    - LocalDateTime createdAt

    + Property(propertyId, hostId, title, location, type)
    + void addAmenity(Amenity)
    + void addPhoto(String url)
    + boolean isAvailable(LocalDate checkIn, LocalDate checkOut)
    + double calculateTotalCost(int nights)
    + void updateRating(double newRating)
    + getters/setters
}
```

### 2.4 PropertyCalendar Class

```java
class PropertyCalendar {
    - String propertyId
    - Map<LocalDate, Boolean> availability  // Date -> isAvailable
    - Set<DateRange> blockedDates           // Booked/blocked ranges

    + PropertyCalendar(propertyId)
    + boolean isAvailable(LocalDate checkIn, LocalDate checkOut)
    + void blockDates(LocalDate checkIn, LocalDate checkOut)
    + void unblockDates(LocalDate checkIn, LocalDate checkOut)
    + List<LocalDate> getAvailableDates(LocalDate start, LocalDate end)
}

class DateRange {
    - LocalDate startDate
    - LocalDate endDate

    + boolean overlaps(DateRange other)
}
```

### 2.5 Booking Class

```java
class Booking {
    - String bookingId
    - String propertyId
    - String guestId
    - String hostId
    - LocalDate checkInDate
    - LocalDate checkOutDate
    - int numberOfGuests
    - int numberOfNights
    - double totalAmount
    - BookingStatus status       // PENDING, CONFIRMED, etc.
    - String paymentId
    - LocalDateTime bookedAt
    - LocalDateTime confirmedAt
    - LocalDateTime cancelledAt
    - String cancellationReason

    + Booking(bookingId, propertyId, guestId, checkIn, checkOut, guests)
    + void confirm()
    + void cancel(String reason)
    + void complete()
    + int calculateNights()
    + getters/setters
}
```

### 2.6 Payment Class

```java
class Payment {
    - String paymentId
    - String bookingId
    - double amount
    - PaymentStatus status       // PENDING, SUCCESS, FAILED
    - String paymentMethod       // CARD, UPI, WALLET
    - LocalDateTime paymentDate
    - String transactionId
    - double refundAmount
    - LocalDateTime refundDate

    + Payment(paymentId, bookingId, amount, method)
    + boolean processPayment()
    + boolean processRefund(double amount)
    + getters/setters
}
```

### 2.7 Review Class

```java
class Review {
    - String reviewId
    - String bookingId
    - String reviewerId          // User who writes review
    - String revieweeId          // User being reviewed (host/guest)
    - String propertyId          // If reviewing property
    - int rating                 // 1-5 stars
    - String comment
    - LocalDateTime reviewDate

    + Review(reviewId, bookingId, reviewerId, rating, comment)
    + getters/setters
}
```

### 2.8 Message Class

```java
class Message {
    - String messageId
    - String senderId
    - String receiverId
    - String bookingId           // Optional: related booking
    - String content
    - LocalDateTime sentAt
    - boolean isRead

    + Message(messageId, senderId, receiverId, content)
    + void markAsRead()
    + getters/setters
}
```

### 2.9 Amenity Enum

```java
enum Amenity {
    WIFI,
    KITCHEN,
    PARKING,
    POOL,
    GYM,
    AIR_CONDITIONING,
    HEATING,
    TV,
    WASHER,
    DRYER,
    WORKSPACE,
    PETS_ALLOWED,
    SMOKING_ALLOWED
}
```

---

## 3. Service Classes (Business Logic)

### 3.1 UserService

```java
class UserService {
    - Map<String, User> users = new HashMap<>();

    + User registerUser(name, email, phone, userType)
    + User getUserById(userId)
    + User getUserByEmail(email)
    + void verifyUser(userId)
    + List<Property> getHostedProperties(userId)
    + List<Booking> getUserBookings(userId)
}
```

### 3.2 PropertyService

```java
class PropertyService {
    - Map<String, Property> properties = new HashMap<>();

    + Property createProperty(hostId, title, description, location, type, price)
    + Property getPropertyById(propertyId)
    + void updateProperty(propertyId, updatedDetails)
    + void deleteProperty(propertyId)
    + void addAmenity(propertyId, amenity)
    + void addPhoto(propertyId, photoUrl)
    + List<Property> getPropertiesByHost(hostId)
    + boolean isAvailable(propertyId, checkIn, checkOut)
}
```

### 3.3 SearchService

```java
class SearchService {
    - PropertyService propertyService

    + List<Property> searchByLocation(Location location, double radiusKm,
                                      LocalDate checkIn, LocalDate checkOut,
                                      int guests, SearchFilters filters)
    + List<Property> searchByCity(String city, LocalDate checkIn,
                                   LocalDate checkOut, int guests)
    + List<Property> filterByPriceRange(List<Property> properties,
                                        double minPrice, double maxPrice)
    + List<Property> filterByPropertyType(List<Property> properties,
                                          PropertyType type)
    + List<Property> filterByAmenities(List<Property> properties,
                                       List<Amenity> requiredAmenities)
    + List<Property> sortByPrice(List<Property> properties, boolean ascending)
    + List<Property> sortByRating(List<Property> properties)
}

class SearchFilters {
    - Double minPrice
    - Double maxPrice
    - PropertyType propertyType
    - List<Amenity> amenities
    - Integer minBedrooms
    - Integer minBathrooms
}
```

### 3.4 BookingService

```java
class BookingService {
    - Map<String, Booking> bookings = new HashMap<>();
    - PropertyService propertyService
    - PaymentService paymentService

    + Booking createBooking(propertyId, guestId, checkIn, checkOut, guests)
    + void confirmBooking(bookingId)        // Host approves or auto-confirm
    + void cancelBooking(bookingId, reason)
    + void completeBooking(bookingId)       // After checkout
    + Booking getBookingById(bookingId)
    + List<Booking> getBookingsByGuest(guestId)
    + List<Booking> getBookingsByProperty(propertyId)
    + boolean checkAvailability(propertyId, checkIn, checkOut)
    - void blockPropertyDates(propertyId, checkIn, checkOut)
    - void unblockPropertyDates(propertyId, checkIn, checkOut)
}
```

### 3.5 PaymentService

```java
class PaymentService {
    - Map<String, Payment> payments = new HashMap<>();

    + Payment processPayment(bookingId, amount, paymentMethod)
    + Payment getPaymentById(paymentId)
    + Payment getPaymentByBooking(bookingId)
    + boolean processRefund(paymentId, refundAmount, CancellationPolicy policy)
    + double calculateRefundAmount(Booking booking, CancellationPolicy policy)
    - boolean chargePayment(amount, paymentMethod)  // Mock payment gateway
}
```

### 3.6 ReviewService

```java
class ReviewService {
    - Map<String, Review> reviews = new HashMap<>();
    - PropertyService propertyService

    + Review addReview(bookingId, reviewerId, revieweeId, propertyId,
                       rating, comment)
    + List<Review> getReviewsForProperty(propertyId)
    + List<Review> getReviewsForUser(userId)
    + double calculateAverageRating(List<Review> reviews)
    + void updatePropertyRating(propertyId)
}
```

### 3.7 MessagingService

```java
class MessagingService {
    - Map<String, List<Message>> conversations = new HashMap<>();
    // Key: "senderId_receiverId"

    + Message sendMessage(senderId, receiverId, content, bookingId)
    + List<Message> getConversation(user1Id, user2Id)
    + void markMessageAsRead(messageId)
    + List<Message> getUnreadMessages(userId)
}
```

---

## 4. Design Patterns Implementation

### 4.1 Strategy Pattern: Cancellation Policy

```java
interface CancellationStrategy {
    double calculateRefund(Booking booking);
}

class FlexibleCancellation implements CancellationStrategy {
    public double calculateRefund(Booking booking) {
        // Full refund if cancelled 24 hours before check-in
        long hoursUntilCheckIn = calculateHours(booking.getCheckInDate());
        if (hoursUntilCheckIn > 24) {
            return booking.getTotalAmount();
        } else {
            return booking.getTotalAmount() * 0.5;  // 50% refund
        }
    }
}

class ModerateCancellation implements CancellationStrategy {
    public double calculateRefund(Booking booking) {
        // Full refund if cancelled 5 days before check-in
        long daysUntilCheckIn = calculateDays(booking.getCheckInDate());
        if (daysUntilCheckIn > 5) {
            return booking.getTotalAmount();
        } else if (daysUntilCheckIn > 1) {
            return booking.getTotalAmount() * 0.5;
        } else {
            return 0;  // No refund
        }
    }
}

class StrictCancellation implements CancellationStrategy {
    public double calculateRefund(Booking booking) {
        // 50% refund if cancelled 7 days before, otherwise no refund
        long daysUntilCheckIn = calculateDays(booking.getCheckInDate());
        if (daysUntilCheckIn > 7) {
            return booking.getTotalAmount() * 0.5;
        } else {
            return 0;
        }
    }
}
```

### 4.2 Strategy Pattern: Pricing Strategy

```java
interface PricingStrategy {
    double calculatePrice(Property property, LocalDate checkIn,
                         LocalDate checkOut, int guests);
}

class DefaultPricing implements PricingStrategy {
    public double calculatePrice(Property property, LocalDate checkIn,
                                LocalDate checkOut, int guests) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return (property.getPricePerNight() * nights)
               + property.getCleaningFee()
               + property.getServiceFee();
    }
}

class WeekendPricing implements PricingStrategy {
    public double calculatePrice(Property property, LocalDate checkIn,
                                LocalDate checkOut, int guests) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        int weekendNights = countWeekendNights(checkIn, checkOut);
        int weekdayNights = nights - weekendNights;

        double weekendRate = property.getPricePerNight() * 1.3;  // 30% markup
        return (weekdayNights * property.getPricePerNight())
               + (weekendNights * weekendRate)
               + property.getCleaningFee()
               + property.getServiceFee();
    }
}
```

### 4.3 Builder Pattern: Property Builder

```java
class PropertyBuilder {
    private String propertyId;
    private String hostId;
    private String title;
    private String description;
    private Location location;
    private PropertyType type;
    private int maxGuests;
    private double pricePerNight;

    public PropertyBuilder setPropertyId(String id) {
        this.propertyId = id;
        return this;
    }

    public PropertyBuilder setHostId(String id) {
        this.hostId = id;
        return this;
    }

    // ... other setters ...

    public Property build() {
        return new Property(propertyId, hostId, title, description,
                           location, type, maxGuests, pricePerNight);
    }
}
```

---

## 5. Key Algorithms

### 5.1 Geolocation Distance (Haversine Formula)

```java
public double calculateDistance(Location loc1, Location loc2) {
    final int EARTH_RADIUS = 6371; // Radius in kilometers

    double lat1Rad = Math.toRadians(loc1.getLatitude());
    double lat2Rad = Math.toRadians(loc2.getLatitude());
    double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
    double deltaLng = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

    double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
               Math.cos(lat1Rad) * Math.cos(lat2Rad) *
               Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS * c;  // Distance in km
}
```

### 5.2 Search by Radius

```java
public List<Property> searchByRadius(Location center, double radiusKm,
                                     LocalDate checkIn, LocalDate checkOut) {
    List<Property> results = new ArrayList<>();

    for (Property property : allProperties) {
        double distance = calculateDistance(center, property.getLocation());

        if (distance <= radiusKm) {
            if (property.isAvailable(checkIn, checkOut)) {
                results.add(property);
            }
        }
    }

    return results;
}
```

### 5.3 Availability Check (No Overlapping Bookings)

```java
public boolean isAvailable(String propertyId, LocalDate checkIn,
                          LocalDate checkOut) {
    List<Booking> existingBookings = getBookingsByProperty(propertyId);

    for (Booking booking : existingBookings) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            // Check if date ranges overlap
            boolean overlaps = !(checkOut.isBefore(booking.getCheckInDate()) ||
                                checkIn.isAfter(booking.getCheckOutDate()));

            if (overlaps) {
                return false;  // Not available
            }
        }
    }

    return true;  // Available
}
```

---

## 6. Class Relationships

### Relationships
- **User** 1:N **Property** (A host can have multiple properties)
- **User** 1:N **Booking** (A guest can have multiple bookings)
- **Property** 1:1 **PropertyCalendar**
- **Property** 1:N **Booking**
- **Property** N:N **Amenity**
- **Property** 1:N **Review**
- **Booking** 1:1 **Payment**
- **Booking** 1:N **Review** (Guest reviews property, host reviews guest)
- **User** N:N **Message** (Users can message each other)

### UML Diagram (Text Representation)

```
┌─────────────┐
│    User     │
├─────────────┤
│ userId      │
│ name        │
│ email       │
│ userType    │
└──────┬──────┘
       │ 1:N
       ▼
┌──────────────┐         1:1      ┌─────────────────┐
│   Property   │◄─────────────────│PropertyCalendar │
├──────────────┤                  ├─────────────────┤
│ propertyId   │                  │ availability    │
│ hostId       │                  │ blockedDates    │
│ title        │                  └─────────────────┘
│ location     │
│ pricePerNight│
└──────┬───────┘
       │ 1:N
       ▼
┌──────────────┐         1:1      ┌─────────────┐
│   Booking    │◄─────────────────│   Payment   │
├──────────────┤                  ├─────────────┤
│ bookingId    │                  │ paymentId   │
│ propertyId   │                  │ amount      │
│ guestId      │                  │ status      │
│ checkInDate  │                  └─────────────┘
│ checkOutDate │
│ status       │
└──────┬───────┘
       │ 1:N
       ▼
┌──────────────┐
│    Review    │
├──────────────┤
│ reviewId     │
│ rating       │
│ comment      │
└──────────────┘
```

---

## 7. SOLID Principles Applied

### Single Responsibility Principle (SRP)
- Each class has one responsibility
- `BookingService` handles bookings only
- `PaymentService` handles payments only
- Separate models from business logic

### Open/Closed Principle (OCP)
- Use Strategy pattern for cancellation policies
- Can add new cancellation policies without modifying existing code

### Liskov Substitution Principle (LSP)
- All `CancellationStrategy` implementations are interchangeable

### Interface Segregation Principle (ISP)
- Specific interfaces for different strategies
- Clients depend only on methods they use

### Dependency Inversion Principle (DIP)
- Services depend on abstractions (interfaces), not concrete classes
- Example: `BookingService` depends on `PaymentService` interface

---

## Summary

This LLD covers:
- ✅ Complete class design with relationships
- ✅ Service layer architecture
- ✅ Design patterns (Strategy, Builder, Singleton)
- ✅ Key algorithms (geolocation, availability check)
- ✅ SOLID principles
- ✅ In-memory data structures (HashMap, ArrayList)

**Next:** See **APPROACH.md** for implementation strategy and then the actual **Java implementation**.
