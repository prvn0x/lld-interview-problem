# Parking Lot - High-Level Design (HLD)

## 1. Problem Statement

Design a multi-floor parking lot management system that:
- Supports multiple vehicle types (Bike, Car, Truck)
- Has different spot sizes (Small, Medium, Large)
- Assigns nearest available compatible spot
- Handles concurrent parking requests safely
- Calculates parking fees based on duration
- Prevents double allocation of spots

---

## 2. Functional Requirements

### Core Features

1. **Vehicle Entry**
   - Accept vehicle with license plate and type
   - Find compatible available spot
   - Assign nearest spot (check floors in order)
   - Issue parking ticket

2. **Vehicle Exit**
   - Accept ticket
   - Calculate parking fee
   - Free the spot
   - Return receipt

3. **Vehicle Types**
   - **Bike** - requires SMALL spot
   - **Car** - requires MEDIUM spot
   - **Truck** - requires LARGE spot

4. **Spot Compatibility**
   - Vehicle can park in spot if: **Spot Size ≥ Vehicle Size**
   - Bike → can use Small, Medium, or Large
   - Car → can use Medium or Large
   - Truck → can use Large only

5. **Multi-Floor Support**
   - Multiple floors in parking lot
   - Each floor has multiple spots of different sizes
   - Allocate spot floor-by-floor (nearest first)

6. **Fee Calculation**
   - Calculate based on parking duration
   - Support multiple pricing strategies
   - Pluggable fee calculation (Strategy Pattern)

7. **Thread Safety**
   - No two vehicles get same spot (atomic reservation)
   - Handle concurrent parking requests
   - Thread-safe spot allocation

---

## 3. Non-Functional Requirements

### Concurrency
- **Thread-safe spot allocation**
- Atomic check-and-reserve operation
- No global locking (use fine-grained locks)

### Performance
- Fast spot lookup (O(1) amortized with queues)
- Minimal locking overhead

### Scalability
- Support hundreds of spots per floor
- Support multiple floors

### Reliability
- Graceful handling when parking full
- No data corruption under concurrent access

---

## 4. System Architecture

```
┌─────────────┐
│   Vehicle   │ enters
└──────┬──────┘
       │
       ▼
┌──────────────────────────┐
│    ParkingLot System     │
│  (check floors in order) │
└──────────┬───────────────┘
           │
    ┌──────┴──────┐
    ▼             ▼
┌────────┐   ┌────────┐   ┌────────┐
│Floor 1 │   │Floor 2 │   │Floor 3 │
└────┬───┘   └────┬───┘   └────┬───┘
     │            │            │
     ▼            ▼            ▼
┌─────────────────────────────────┐
│  Spot Queues by Size            │
│  - SMALL spots queue            │
│  - MEDIUM spots queue           │
│  - LARGE spots queue            │
└─────────────────────────────────┘
```

---

## 5. Core Components

### 5.1 ParkingLot
- Main system coordinator
- Manages multiple floors
- Issues tickets
- Calculates fees using strategy

### 5.2 Floor
- Contains multiple parking spots
- Maintains available spot queues by size
- Provides thread-safe spot allocation

### 5.3 ParkingSpot
- Individual parking space
- Has size (Small/Medium/Large)
- Has status (Available/Occupied)
- Thread-safe reservation using lock

### 5.4 Vehicle
- Has type (Bike/Car/Truck)
- Has license plate

### 5.5 Ticket
- Issued on entry
- Contains vehicle, spot, entry time
- Used for exit and fee calculation

### 5.6 FeeStrategy (Strategy Pattern)
- Interface for fee calculation
- Different implementations:
  - HourlyFeeStrategy
  - WeekendFeeStrategy
  - SurgeFeeStrategy (future)

---

## 6. Key Algorithms

### 6.1 Spot Allocation Algorithm

**Goal:** Find nearest available compatible spot

**Process:**
```
1. For each floor (in order 1, 2, 3...):
   2. Get compatible spot from floor
   3. If spot found:
      4. Reserve atomically
      5. Return spot
   6. If no spot on this floor, try next floor
7. If no spot on any floor:
   8. Return null (parking full)
```

**Floor-level allocation:**
```
1. Determine required spot size from vehicle type
2. For each spot size (SMALL, MEDIUM, LARGE):
   3. If this size can fit vehicle:
      4. Poll spot from queue
      5. Try to reserve atomically
      6. If success, return spot
      7. If fail (race condition), try next
8. Return null if no compatible spot
```

---

### 6.2 Atomic Reservation

**Problem:** Multiple threads trying to park simultaneously

**Solution:** Thread-safe check-and-reserve

```java
lock.lock()
try:
    if (spot.status == AVAILABLE && spot.canFit(vehicle)):
        spot.status = OCCUPIED
        spot.vehicle = vehicle
        return true
    return false
finally:
    lock.unlock()
```

**Key Points:**
- Use ReentrantLock per spot (fine-grained locking)
- Check availability INSIDE lock
- Change state INSIDE lock
- Atomic operation - no race condition

---

### 6.3 Spot Compatibility Check

**Rule:** Vehicle can park if **Spot Size ≥ Vehicle Size**

**Numeric mapping:**
- SMALL = 1
- MEDIUM = 2
- LARGE = 3

**Compatibility matrix:**
```
Vehicle  | SMALL | MEDIUM | LARGE
---------|-------|--------|-------
BIKE     |  ✓    |   ✓    |   ✓
CAR      |  ✗    |   ✓    |   ✓
TRUCK    |  ✗    |   ✗    |   ✓
```

**Implementation:**
```java
boolean canFit(SpotSize spotSize, VehicleType vehicleType) {
    return spotSize.value >= vehicleType.requiredSize.value;
}
```

---

## 7. Thread Safety Strategy

### Fine-Grained Locking
- **Per-spot lock** (ReentrantLock in ParkingSpot)
- **NOT global parking lot lock**
- Allows multiple vehicles to park simultaneously on different spots

### Concurrent Collections
- **ConcurrentLinkedQueue** for spot queues
- **ConcurrentHashMap** for active tickets
- Thread-safe without explicit locking

### Atomic Operations
- Reserve operation is atomic
- No check-then-act race condition

---

## 8. Fee Calculation Strategy

### Strategy Pattern Benefits
- Pluggable pricing logic
- Easy to add new strategies
- No modification to ParkingLot code

### Strategies

**1. HourlyFeeStrategy (Default)**
```
Bike:  $2/hour
Car:   $5/hour
Truck: $10/hour
```

**2. WeekendFeeStrategy**
```
Base rate × 1.5 on Saturday/Sunday
```

**3. Future Strategies**
- SurgeFeeStrategy (peak hours)
- FlatFeeStrategy (daily/monthly pass)
- DynamicFeeStrategy (based on demand)

---

## 9. Data Flow

### Entry Flow
```
1. Vehicle arrives
2. ParkingLot.parkVehicle(vehicle)
3. For each floor:
   - floor.getAvailableSpot(vehicle)
   - Check spot queues by size
   - Poll compatible spot
   - spot.reserve(vehicle) [atomic]
4. Create Ticket
5. Store in activeTickets map
6. Return ticket to driver
```

### Exit Flow
```
1. Vehicle exits with ticket
2. ParkingLot.exitVehicle(ticketId)
3. Get ticket from activeTickets
4. Calculate duration (exit - entry)
5. feeStrategy.calculateFee(ticket, duration)
6. spot.vacate()
7. floor.freeSpot(spot) - add back to queue
8. Return fee
```

---

## 10. Edge Cases & Error Handling

### Parking Full
- All spots occupied
- parkVehicle() returns null
- Display "FULL" sign
- Graceful degradation

### Invalid Ticket
- Ticket not found in activeTickets
- Throw IllegalArgumentException
- Log error

### Concurrent Access
- Multiple threads parking simultaneously
- Handled by atomic reservation
- No double allocation possible

### Spot Not Available During Reservation
- Another thread got it first
- Try next spot in queue
- Keep trying until found or queue empty

---

## 11. Scalability Considerations

### Current Design (Single Parking Lot)
- Supports up to ~1000 spots efficiently
- Fine-grained locking allows high concurrency

### Future Enhancements for Scale

**1. Multiple Parking Lots**
- Distribute geographically
- Each lot independent
- Central directory service

**2. Distributed Locking**
- Replace ReentrantLock with Redis locks
- Allows distributed systems

**3. Event-Driven Architecture**
- Entry/Exit events to message queue
- Async processing
- Separate services for analytics

**4. Database Per Lot**
- Each parking lot has own DB
- Reduce contention
- Easy to scale horizontally

**5. Caching Layer**
- Cache floor availability in Redis
- Reduce database queries
- TTL-based invalidation

---

## 12. Monitoring & Observability

### Metrics to Track
- Occupancy rate per floor
- Average parking duration
- Revenue per hour/day
- Peak usage times
- Concurrent parking requests

### Alerts
- Parking lot near full (>90%)
- System errors
- Slow spot allocation

---

## 13. Technology Stack (Suggested)

**Backend:**
- Java with concurrent collections
- Spring Boot (for production)

**Database:**
- PostgreSQL (relational)
- Redis (caching, distributed locks)

**Monitoring:**
- Prometheus (metrics)
- Grafana (dashboards)

---

## 14. Comparison with Real Systems

### Similar To
- Mall parking systems
- Airport parking
- Hotel valet systems

### Differences
- Real systems often use sensors (IoT)
- RFID/barcode scanning
- Mobile app integration
- Real-time occupancy display

---

## Summary

**Key Design Decisions:**

1. **Queue-based allocation** for fast spot lookup
2. **Fine-grained locking** for concurrency
3. **Strategy Pattern** for flexible fee calculation
4. **Floor-by-floor search** for nearest spot
5. **Atomic reservation** to prevent double allocation

**Next:** See **LLD.md** for detailed class design and **APPROACH.md** for implementation strategy.
