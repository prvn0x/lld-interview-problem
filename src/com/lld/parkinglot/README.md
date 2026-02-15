# Parking Lot - Low-Level Design Implementation

## 📝 Problem Statement

Design a multi-floor parking lot management system where:
- Supports multiple vehicle types (Bike, Car, Truck)
- Has different spot sizes (Small, Medium, Large)
- Assigns nearest available compatible spot
- Handles concurrent parking requests safely
- Calculates parking fees based on duration
- Prevents double allocation of spots

---

## 🎯 Features Implemented

### ✅ Core Features
1. **Multi-Floor Support**
   - Multiple floors with configurable spots
   - Floor-by-floor nearest spot allocation
   - Real-time availability tracking per floor

2. **Vehicle Type Management**
   - BIKE - requires SMALL spot (can use MEDIUM or LARGE)
   - CAR - requires MEDIUM spot (can use LARGE)
   - TRUCK - requires LARGE spot only

3. **Spot Compatibility**
   - Vehicle can park if: **Spot Size ≥ Vehicle Size**
   - Numeric mapping: SMALL(1), MEDIUM(2), LARGE(3)
   - Automatic best-fit spot selection

4. **Thread-Safe Parking**
   - Atomic spot reservation using ReentrantLock
   - No double allocation even with concurrent requests
   - Fine-grained locking (per spot, not global)
   - Demonstrated with 10 concurrent parking threads

5. **Queue-Based Allocation**
   - ConcurrentLinkedQueue for available spots by size
   - O(1) amortized spot lookup
   - Automatic re-queuing when spot freed

6. **Fee Calculation (Strategy Pattern)**
   - Pluggable pricing strategies
   - HourlyFeeStrategy: Different rates for Bike/Car/Truck
   - WeekendFeeStrategy: 50% markup on weekends
   - Easy to add new strategies (Surge, Flat, Dynamic)

7. **Ticket Management**
   - Unique ticket issued on entry
   - Stores vehicle, spot, entry time
   - Used for exit and fee calculation

8. **Graceful Full Handling**
   - Returns null when parking full
   - Clear status display
   - No crashes or exceptions

---

## 🏗️ Architecture

### Package Structure
```
com.lld.parkinglot/
├── enums/
│   ├── VehicleType.java        # BIKE, CAR, TRUCK with required sizes
│   ├── SpotSize.java           # SMALL, MEDIUM, LARGE with canFit() logic
│   └── SpotStatus.java         # AVAILABLE, OCCUPIED
├── models/
│   ├── Vehicle.java            # Simple vehicle data
│   ├── ParkingSpot.java        # ⭐ Thread-safe with ReentrantLock
│   ├── Ticket.java             # Issued on entry
│   ├── Floor.java              # ⭐ Queue-based allocation
│   └── ParkingLot.java         # ⭐ Main system coordinator
├── strategies/
│   ├── FeeStrategy.java        # Interface (Strategy Pattern)
│   ├── HourlyFeeStrategy.java  # Default pricing
│   └── WeekendFeeStrategy.java # Weekend markup
└── Main.java                    # ✅ RUN THIS - Comprehensive demo
```

### Design Patterns Used
- **Strategy Pattern**: Pluggable fee calculation (FeeStrategy)
- **Thread Safety**: ReentrantLock for atomic operations
- **Enum with Behavior**: SpotSize.canFit()
- **Queue-based Allocation**: ConcurrentLinkedQueue

### Key Algorithms
1. **Spot Compatibility Check**: O(1) numeric comparison
2. **Atomic Reservation**: Lock-based check-and-reserve
3. **Queue-based Search**: O(1) amortized with queues
4. **Floor-by-Floor Allocation**: O(F × S) where F=floors, S=spots

---

## 🚀 How to Run

### Using IntelliJ IDEA (Recommended)
1. Open project in IntelliJ
2. Navigate to `src/com/lld/parkinglot/Main.java`
3. Right-click → **Run 'Main.main()'**
4. See comprehensive demo output!

### Using Command Line
```bash
# From project root directory
cd /Users/praveen.singh/Desktop/lld-interview-problems

# Compile
javac -d out -sourcepath src src/com/lld/parkinglot/Main.java

# Run
java -cp out com.lld.parkinglot.Main
```

---

## 📊 Demo Flow

The `Main.java` demonstrates:

### 8-Step Comprehensive Demo:

1. **Initialize Parking Lot**
   - 3 floors
   - Floor 1: 10 spots (4 SMALL, 4 MEDIUM, 2 LARGE)
   - Floor 2: 8 spots (3 SMALL, 3 MEDIUM, 2 LARGE)
   - Floor 3: 6 spots (2 SMALL, 2 MEDIUM, 2 LARGE)

2. **Park Different Vehicle Types**
   - 2 Bikes (go to SMALL spots)
   - 2 Cars (go to MEDIUM spots)
   - 1 Truck (goes to LARGE spot)
   - Display updated status

3. **Test Spot Compatibility**
   - Verify BIKE can fit in SMALL: true ✓
   - Verify CAR cannot fit in SMALL: false ✓
   - Verify TRUCK cannot fit in MEDIUM: false ✓
   - Verify TRUCK can fit in LARGE: true ✓

4. **Exit Vehicles (Fee Calculation)**
   - Calculate parking duration
   - Apply HourlyFeeStrategy
   - Display fee breakdown

5. **Change Fee Strategy (Strategy Pattern)**
   - Switch to WeekendFeeStrategy
   - Park and exit vehicle
   - Show 50% weekend markup

6. **Test Full Parking Lot**
   - Fill all remaining spots
   - Try to park one more vehicle
   - Return null gracefully

7. **Test Thread Safety (Concurrent Parking)**
   - Free 5 spots
   - Launch 10 concurrent parking threads
   - All attempts handled safely
   - No double allocation
   - Unique spots assigned

8. **Exit All Vehicles**
   - Calculate total revenue
   - Free all spots
   - Show final status (all available)

---

## 🧮 Key Logic Explained

### 1. Spot Compatibility (Core Algorithm)

```java
public enum SpotSize {
    SMALL(1), MEDIUM(2), LARGE(3);

    private int value;

    public boolean canFit(SpotSize vehicleSize) {
        return this.value >= vehicleSize.value;
    }
}
```

**Logic**:
- SMALL(1) ≥ BIKE(1) = true ✓
- SMALL(1) ≥ CAR(2) = false ✗
- MEDIUM(2) ≥ BIKE(1) = true ✓
- LARGE(3) ≥ TRUCK(3) = true ✓

### 2. Atomic Reservation (Thread Safety)

```java
public boolean reserve(Vehicle vehicle) {
    lock.lock();
    try {
        if (status == SpotStatus.AVAILABLE &&
            size.canFit(vehicle.getType().getRequiredSize())) {
            status = SpotStatus.OCCUPIED;
            currentVehicle = vehicle;
            return true;
        }
        return false;
    } finally {
        lock.unlock();
    }
}
```

**Why this works:**
- Check and reserve happen atomically inside lock
- Only one thread can execute at a time
- No race condition possible
- Always unlocks (finally block)

### 3. Queue-Based Allocation (Floor)

```java
public ParkingSpot getAvailableSpot(Vehicle vehicle) {
    SpotSize requiredSize = vehicle.getType().getRequiredSize();

    for (SpotSize size : SpotSize.values()) {
        if (size.canFit(requiredSize)) {
            Queue<ParkingSpot> queue = availableSpotsBySize.get(size);

            ParkingSpot spot = queue.poll();
            while (spot != null) {
                if (spot.reserve(vehicle)) {
                    return spot;  // Success!
                }
                spot = queue.poll();  // Try next
            }
        }
    }
    return null;  // No compatible spot
}
```

**Process:**
1. Get required size from vehicle type
2. Try each spot size that can fit vehicle
3. Poll spot from queue
4. Try atomic reservation
5. If fail (race condition), try next spot
6. Return spot if successful, null if none available

### 4. Fee Calculation (Strategy Pattern)

**HourlyFeeStrategy:**
```java
double calculateFee(Ticket ticket, Duration duration) {
    long hours = duration.toHours();
    if (duration.toMinutesPart() > 0) hours++;  // Round up
    if (hours == 0) hours = 1;  // Minimum 1 hour

    double hourlyRate = getRate(ticket.getVehicle().getType());
    // BIKE: $2/hour, CAR: $5/hour, TRUCK: $10/hour

    return hours * hourlyRate;
}
```

**WeekendFeeStrategy:**
```java
double calculateFee(Ticket ticket, Duration duration) {
    double baseFee = hourlyStrategy.calculateFee(ticket, duration);

    DayOfWeek day = ticket.getEntryTime().getDayOfWeek();
    if (day == SATURDAY || day == SUNDAY) {
        return baseFee * 1.5;  // 50% markup
    }
    return baseFee;
}
```

---

## 🗂️ Data Models

### Vehicle
```java
- String licensePlate       // "BIKE-001", "CAR-123"
- VehicleType type          // BIKE, CAR, TRUCK
```

### ParkingSpot
```java
- String spotId             // "F1-S3" (Floor 1, Small 3)
- SpotSize size             // SMALL, MEDIUM, LARGE
- int floorNumber           // 1, 2, 3
- SpotStatus status         // AVAILABLE, OCCUPIED
- Vehicle currentVehicle    // Currently parked vehicle
- ReentrantLock lock        // ⭐ For thread safety
```

### Ticket
```java
- String ticketId           // UUID
- Vehicle vehicle           // Parked vehicle
- ParkingSpot spot          // Assigned spot
- LocalDateTime entryTime   // Entry timestamp
- LocalDateTime exitTime    // Exit timestamp (set on exit)
- double fee                // Calculated fee
```

### Floor
```java
- int floorNumber
- Map<String, ParkingSpot> allSpots
- Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize
```

### ParkingLot
```java
- String parkingLotId
- List<Floor> floors
- Map<String, Ticket> activeTickets  // ConcurrentHashMap
- FeeStrategy feeStrategy            // Pluggable
```

---

## 💾 In-Memory Storage

All data stored in concurrent collections:
```java
// ParkingLot
Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();

// Floor
Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize = new ConcurrentHashMap<>();
Queue<ParkingSpot> = new ConcurrentLinkedQueue<>();

// ParkingSpot
ReentrantLock lock = new ReentrantLock();
```

---

## 🔒 Thread Safety Analysis

### Problem: Race Condition
```
Time  Thread 1              Thread 2
----  -----------------     -----------------
T1    Check spot available
T2                          Check spot available
T3    Reserve spot
T4                          Reserve spot
T5    Both think they got the same spot! ❌
```

### Solution: Atomic Reservation
```
Time  Thread 1              Thread 2
----  -----------------     -----------------
T1    lock.lock()
T2    Check available       lock.lock() [BLOCKED]
T3    Reserve
T4    lock.unlock()
T5                          Check available [already occupied]
T6                          Return false ✓
T7                          lock.unlock()
```

### Why ReentrantLock (not synchronized)?
- More flexible than synchronized
- Explicit lock/unlock
- Can try-lock with timeout (future enhancement)
- Better performance under high contention
- Fine-grained locking (per spot, not global)

---

## 🎯 Interview Discussion Points

### Concurrency Handled
1. **ReentrantLock per spot** - Fine-grained locking
2. **ConcurrentHashMap** - Thread-safe ticket storage
3. **ConcurrentLinkedQueue** - Thread-safe spot queues
4. **Atomic operations** - Check-and-reserve together
5. **Demonstrated** - 10 concurrent threads, no double allocation

### Scalability Improvements
1. **Distributed System**
   - Multiple parking lot instances
   - Distributed locks (Redis, Zookeeper)
   - Database per parking lot

2. **Performance Optimization**
   - Cache floor availability in Redis
   - Real-time display with WebSocket
   - Event-driven architecture

3. **Advanced Features**
   - Pre-booking/reservation system
   - Dynamic pricing based on demand
   - IoT sensors for automatic detection
   - Mobile app with QR code tickets
   - Analytics dashboard

### Trade-offs Made
- **In-memory vs Database**: Fast but not persistent
- **Fine-grained vs Global lock**: Better concurrency but more complex
- **Queue vs List**: O(1) but unordered
- **Strategy Pattern vs If-else**: Extensible but more classes

### Production Considerations
- Database persistence (PostgreSQL)
- Transaction management (ACID properties)
- Distributed locking (Redis)
- Monitoring and logging
- Payment gateway integration
- Real-time occupancy display
- Mobile app integration
- Admin dashboard
- Reports and analytics

---

## 🎓 Time Complexity Analysis

| Operation | Time Complexity | Space Complexity |
|-----------|----------------|------------------|
| Park vehicle | O(F × S) | O(1) |
| Exit vehicle | O(1) | O(1) |
| Check compatibility | O(1) | O(1) |
| Reserve spot | O(1) | O(1) |
| Free spot | O(1) | O(1) |
| Get available spots | O(1) per floor | O(1) |

Where:
- F = number of floors (typically small, e.g., 3-10)
- S = spots per floor polled before finding available one (amortized O(1))

---

## 🧪 Test Cases Covered

### Functionality Tests
- ✅ Park bike in small spot
- ✅ Park car in medium spot
- ✅ Park truck in large spot
- ✅ Bike can use medium/large spots
- ✅ Car cannot use small spot
- ✅ Exit and fee calculation
- ✅ Strategy pattern fee change
- ✅ Full parking lot handling

### Thread Safety Tests
- ✅ 10 concurrent parking threads
- ✅ No double allocation
- ✅ Unique spots assigned
- ✅ Graceful race condition handling

### Edge Cases
- ✅ Parking lot full
- ✅ Invalid ticket
- ✅ Concurrent access
- ✅ Round-up duration (partial hours)
- ✅ Minimum 1 hour fee

---

## 📚 Learning Resources

- [ReentrantLock Documentation](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/ReentrantLock.html)
- [ConcurrentHashMap](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html)
- [Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Concurrent Programming in Java](https://www.oreilly.com/library/view/concurrent-programming-in/0321349601/)

---

## ✅ Checklist for Interview

- [x] Clarified requirements (vehicles, spots, thread safety)
- [x] Designed core entities (Vehicle, Spot, Floor, ParkingLot)
- [x] Implemented spot compatibility logic (critical!)
- [x] Implemented atomic reservation with ReentrantLock
- [x] Implemented queue-based allocation
- [x] Applied Strategy Pattern for fees
- [x] Handled edge cases (full, concurrent, invalid)
- [x] Created comprehensive demo with concurrent test
- [x] Discussed scalability and trade-offs

---

## 🎯 Key Interview Points

**When asked about thread safety:**
"I used ReentrantLock for atomic spot reservation. The check-and-reserve operation happens atomically inside the lock, preventing double allocation even when multiple threads try to park simultaneously."

**When asked about compatibility:**
"I use numeric size mapping: SMALL(1), MEDIUM(2), LARGE(3). A spot can fit a vehicle if spot size ≥ vehicle size. This is O(1) constant-time check."

**When asked about Strategy Pattern:**
"FeeStrategy makes pricing pluggable. I can easily add new strategies like surge pricing or monthly passes without modifying ParkingLot code. This follows Open/Closed Principle."

**When asked about scalability:**
"For distributed systems, I'd replace ReentrantLock with Redis locks, use database transactions, and implement event-driven architecture for analytics and notifications."

---

## 🏆 Success Criteria

You've successfully understood this design if you can:
1. ✅ Explain spot compatibility logic (size comparison)
2. ✅ Implement atomic reservation with ReentrantLock
3. ✅ Apply Strategy Pattern for fee calculation
4. ✅ Handle concurrent parking without double allocation
5. ✅ Explain thread safety trade-offs

---

**Author**: Praveen Singh
**Date**: February 2026
**Purpose**: LLD Interview Preparation

**Note**: This implementation demonstrates core concepts using in-memory storage. In production, replace with database persistence, distributed locking, and proper transaction management.
