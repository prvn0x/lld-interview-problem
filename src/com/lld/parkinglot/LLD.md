# Parking Lot - Low-Level Design (LLD)

## 1. Class Diagram Overview

### Enums
- VehicleType
- SpotSize
- SpotStatus

### Models
- Vehicle
- ParkingSpot
- Ticket
- Floor
- ParkingLot

### Strategies
- FeeStrategy (interface)
- HourlyFeeStrategy
- WeekendFeeStrategy

---

## 2. Detailed Class Design

### 2.1 VehicleType Enum

```java
enum VehicleType {
    BIKE(SpotSize.SMALL),      // Requires SMALL spot
    CAR(SpotSize.MEDIUM),      // Requires MEDIUM spot
    TRUCK(SpotSize.LARGE);     // Requires LARGE spot

    private SpotSize requiredSize;

    VehicleType(SpotSize requiredSize) {
        this.requiredSize = requiredSize;
    }

    + SpotSize getRequiredSize()
}
```

---

### 2.2 SpotSize Enum

```java
enum SpotSize {
    SMALL(1),
    MEDIUM(2),
    LARGE(3);

    private int value;

    SpotSize(int value) {
        this.value = value;
    }

    // Check if this spot can accommodate a vehicle
    + boolean canFit(SpotSize vehicleSize) {
        return this.value >= vehicleSize.value;
    }
}
```

**Compatibility Logic:**
```
SMALL(1).canFit(SMALL(1))   = true   ✓
SMALL(1).canFit(MEDIUM(2))  = false  ✗
MEDIUM(2).canFit(SMALL(1))  = true   ✓
MEDIUM(2).canFit(MEDIUM(2)) = true   ✓
LARGE(3).canFit(TRUCK(3))   = true   ✓
```

---

### 2.3 Vehicle Class

```java
class Vehicle {
    - String licensePlate
    - VehicleType type

    + Vehicle(licensePlate, type)
    + String getLicensePlate()
    + VehicleType getType()
}
```

Simple data holder. No complex logic.

---

### 2.4 ParkingSpot Class (Thread-Safe!)

```java
class ParkingSpot {
    - String spotId                  // e.g., "F1-S3" (Floor 1, Small 3)
    - SpotSize size
    - int floorNumber
    - SpotStatus status              // AVAILABLE, OCCUPIED
    - Vehicle currentVehicle
    - ReentrantLock lock             // ⭐ For thread safety

    + ParkingSpot(spotId, size, floorNumber)

    // Check if vehicle fits
    + boolean canFit(Vehicle vehicle)

    // ⭐ Atomic reservation - CRITICAL METHOD
    + boolean reserve(Vehicle vehicle) {
        lock.lock()
        try:
            if (status == AVAILABLE && canFit(vehicle)):
                status = OCCUPIED
                currentVehicle = vehicle
                return true
            return false
        finally:
            lock.unlock()
    }

    // Free the spot
    + void vacate() {
        lock.lock()
        try:
            status = AVAILABLE
            currentVehicle = null
        finally:
            lock.unlock()
    }

    // Check availability (thread-safe)
    + boolean isAvailable() {
        lock.lock()
        try:
            return status == AVAILABLE
        finally:
            lock.unlock()
    }

    + getters/setters
}
```

**Key Points:**
- ReentrantLock ensures atomic check-and-reserve
- No race condition when multiple threads park
- Fine-grained locking (per spot, not global)

---

### 2.5 Ticket Class

```java
class Ticket {
    - String ticketId              // UUID
    - Vehicle vehicle
    - ParkingSpot spot
    - LocalDateTime entryTime
    - LocalDateTime exitTime
    - double fee

    + Ticket(vehicle, spot)
    + void setExitTime(exitTime)
    + void setFee(fee)
    + getters
}
```

Simple ticket issued on entry, used for exit and fee calculation.

---

### 2.6 Floor Class

```java
class Floor {
    - int floorNumber
    - Map<String, ParkingSpot> allSpots                    // spotId → spot
    - Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize

    + Floor(floorNumber)

    // Add spot to floor
    + void addSpot(ParkingSpot spot) {
        allSpots.put(spot.getId(), spot)
        availableSpotsBySize.get(spot.getSize()).offer(spot)
    }

    // ⭐ Get available spot for vehicle (CRITICAL METHOD)
    + ParkingSpot getAvailableSpot(Vehicle vehicle) {
        SpotSize requiredSize = vehicle.getType().getRequiredSize()

        // Try each spot size (SMALL, MEDIUM, LARGE)
        for (SpotSize size : SpotSize.values()) {
            if (size.canFit(requiredSize)) {
                Queue<ParkingSpot> queue = availableSpotsBySize.get(size)

                // Try spots from queue
                ParkingSpot spot = queue.poll()
                while (spot != null) {
                    // Atomic reservation attempt
                    if (spot.reserve(vehicle)) {
                        return spot  // Success!
                    }
                    // Failed (race condition) - try next
                    spot = queue.poll()
                }
            }
        }
        return null  // No available spot
    }

    // Free spot (add back to queue)
    + void freeSpot(ParkingSpot spot) {
        spot.vacate()
        availableSpotsBySize.get(spot.getSize()).offer(spot)
    }

    + int getAvailableCount(SpotSize size)
    + int getTotalAvailableSpots()
}
```

**Key Points:**
- ConcurrentLinkedQueue for thread-safe queue ops
- Polls spots and tries atomic reservation
- If reservation fails (another thread got it), tries next spot

---

### 2.7 ParkingLot Class

```java
class ParkingLot {
    - String parkingLotId
    - List<Floor> floors
    - Map<String, Ticket> activeTickets      // ticketId → ticket
    - FeeStrategy feeStrategy

    + ParkingLot(parkingLotId, numFloors)

    // ⭐ Park vehicle (MAIN METHOD)
    + Ticket parkVehicle(Vehicle vehicle) {
        // Check floors in order (nearest first)
        for (Floor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(vehicle)

            if (spot != null) {
                // Spot found and reserved!
                Ticket ticket = new Ticket(vehicle, spot)
                activeTickets.put(ticket.getId(), ticket)
                return ticket
            }
        }

        // No spot available
        return null  // Parking FULL
    }

    // ⭐ Exit vehicle (MAIN METHOD)
    + double exitVehicle(String ticketId) {
        Ticket ticket = activeTickets.remove(ticketId)
        if (ticket == null) throw new IllegalArgumentException()

        ticket.setExitTime(now())

        // Calculate duration and fee
        Duration duration = Duration.between(ticket.getEntryTime(), ticket.getExitTime())
        double fee = feeStrategy.calculateFee(ticket, duration)
        ticket.setFee(fee)

        // Free spot
        ParkingSpot spot = ticket.getSpot()
        Floor floor = floors.get(spot.getFloorNumber() - 1)
        floor.freeSpot(spot)

        return fee
    }

    // Strategy Pattern - pluggable fee calculation
    + void setFeeStrategy(FeeStrategy strategy) {
        this.feeStrategy = strategy
    }

    + Floor getFloor(int number)
    + void displayStatus()
}
```

**Key Points:**
- Floor-by-floor allocation (nearest first)
- Uses Strategy Pattern for fee calculation
- Thread-safe via ConcurrentHashMap for activeTickets

---

## 3. Strategy Pattern (Fee Calculation)

### 3.1 FeeStrategy Interface

```java
interface FeeStrategy {
    double calculateFee(Ticket ticket, Duration parkingDuration);
    String getStrategyName();
}
```

---

### 3.2 HourlyFeeStrategy

```java
class HourlyFeeStrategy implements FeeStrategy {
    private static final double BIKE_RATE = 2.0
    private static final double CAR_RATE = 5.0
    private static final double TRUCK_RATE = 10.0

    + double calculateFee(Ticket ticket, Duration duration) {
        long hours = duration.toHours()
        if (duration.toMinutesPart() > 0) hours++  // Round up

        if (hours == 0) hours = 1  // Minimum 1 hour

        double hourlyRate = getRate(ticket.getVehicle().getType())
        return hours * hourlyRate
    }

    - double getRate(VehicleType type) {
        switch (type):
            BIKE: return BIKE_RATE
            CAR: return CAR_RATE
            TRUCK: return TRUCK_RATE
    }
}
```

---

### 3.3 WeekendFeeStrategy

```java
class WeekendFeeStrategy implements FeeStrategy {
    private HourlyFeeStrategy baseStrategy

    + double calculateFee(Ticket ticket, Duration duration) {
        double baseFee = baseStrategy.calculateFee(ticket, duration)

        // Check if entry was on weekend
        DayOfWeek day = ticket.getEntryTime().getDayOfWeek()
        boolean isWeekend = (day == SATURDAY || day == SUNDAY)

        if (isWeekend):
            return baseFee * 1.5  // 50% markup
        return baseFee
    }
}
```

---

## 4. Thread Safety Analysis

### Problem: Race Condition

**Scenario without locking:**
```
Time  Thread 1              Thread 2
----  -----------------     -----------------
T1    Check spot available
T2                          Check spot available
T3    Reserve spot
T4                          Reserve spot
T5    Both think they got the same spot! ❌
```

### Solution: Atomic Reservation with Lock

```
Time  Thread 1              Thread 2
----  -----------------     -----------------
T1    lock.lock()
T2    Check available
T3    Reserve              lock.lock() [BLOCKED]
T4    lock.unlock()
T5                          Check available [already occupied]
T6                          Return false ✓
T7                          lock.unlock()
```

**Key:** Check and reserve happen atomically inside lock.

---

## 5. Key Algorithms

### 5.1 Spot Allocation

**Pseudocode:**
```
function parkVehicle(vehicle):
    for each floor in floors:
        spot = floor.getAvailableSpot(vehicle)
        if spot != null:
            ticket = new Ticket(vehicle, spot)
            return ticket
    return null  // Full

function getAvailableSpot(vehicle):
    requiredSize = vehicle.type.requiredSize

    for each spotSize in [SMALL, MEDIUM, LARGE]:
        if spotSize.canFit(requiredSize):
            queue = availableSpotsBySize[spotSize]

            spot = queue.poll()
            while spot != null:
                if spot.reserve(vehicle):  // Atomic
                    return spot
                spot = queue.poll()  // Try next

    return null  // No compatible spot
```

**Time Complexity:**
- Best case: O(1) - spot available immediately
- Worst case: O(S) where S = spots per floor
- Amortized: O(1) with queues

---

### 5.2 Compatibility Check

```
function canFit(spotSize, vehicleSize):
    return spotSize.value >= vehicleSize.value

Examples:
  canFit(SMALL(1), BIKE(1))   = true   ✓
  canFit(SMALL(1), CAR(2))    = false  ✗
  canFit(MEDIUM(2), BIKE(1))  = true   ✓
  canFit(LARGE(3), TRUCK(3))  = true   ✓
```

---

## 6. Class Relationships (UML)

```
┌──────────────┐
│  ParkingLot  │
├──────────────┤
│ - floors     │
│ - tickets    │
│ - strategy   │◄──────┐
└──────┬───────┘       │
       │ 1:N           │ uses
       ▼               │
┌──────────────┐       │
│    Floor     │       │
├──────────────┤       │
│ - spots      │       │
│ - queues     │       │
└──────┬───────┘       │
       │ 1:N           │
       ▼               │
┌──────────────┐       │
│ ParkingSpot  │       │
├──────────────┤       │
│ - lock ⭐    │       │
│ - vehicle    │       │
└──────┬───────┘       │
       │ 1:1           │
       ▼               │
┌──────────────┐       │
│   Vehicle    │       │
├──────────────┤       │
│ - type       │       │
└──────────────┘       │
                       │
┌──────────────┐       │
│  Ticket      │       │
├──────────────┤       │
│ - spot       │       │
│ - vehicle    │       │
└──────────────┘       │
                       │
┌──────────────┐       │
│ FeeStrategy  │◄──────┘
├──────────────┤
│ + calculate()│
└──────┬───────┘
       │ implements
       ▼
┌──────────────────┐
│HourlyFeeStrategy │
│WeekendFeeStrategy│
└──────────────────┘
```

---

## 7. SOLID Principles Applied

### Single Responsibility Principle (SRP)
- ParkingSpot: manages one spot
- Floor: manages one floor
- ParkingLot: coordinates system
- Each class has ONE job

### Open/Closed Principle (OCP)
- FeeStrategy: can add new strategies without modifying ParkingLot
- Open for extension, closed for modification

### Liskov Substitution Principle (LSP)
- All FeeStrategy implementations are interchangeable

### Interface Segregation Principle (ISP)
- FeeStrategy has only one method - clients use only what they need

### Dependency Inversion Principle (DIP)
- ParkingLot depends on FeeStrategy interface, not concrete classes

---

## 8. Concurrency Design

### Collections Used

| Collection | Purpose | Thread-Safe? |
|------------|---------|--------------|
| ConcurrentLinkedQueue | Spot queues | Yes ✓ |
| ConcurrentHashMap | Active tickets | Yes ✓ |
| ReentrantLock | Spot reservation | N/A (synchronization primitive) |

### Why ReentrantLock (not synchronized)?

**Advantages:**
- More flexible than synchronized
- Can try-lock with timeout
- Explicit lock/unlock
- Better performance under high contention

---

## 9. Design Patterns Used

1. **Strategy Pattern** - FeeStrategy
2. **Enum with Behavior** - SpotSize.canFit()
3. **Queue-based Allocation** - ConcurrentLinkedQueue
4. **Fine-grained Locking** - ReentrantLock per spot

---

## 10. Edge Cases Handled

1. **Parking Full** - Returns null, graceful
2. **Invalid Ticket** - Throws exception
3. **Concurrent Parking** - Atomic reservation prevents double allocation
4. **Spot Not Available** - Tries next spot in queue
5. **Wrong Vehicle Type** - Compatibility check prevents

---

## Summary

This LLD covers:
- ✅ Complete class design with thread safety
- ✅ Atomic reservation using ReentrantLock
- ✅ Queue-based allocation for performance
- ✅ Strategy Pattern for pluggable fees
- ✅ SOLID principles
- ✅ Concurrency handling

**Next:** See **APPROACH.md** for implementation strategy and then run **Main.java**.
