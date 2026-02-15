# Parking Lot - Implementation Approach

## Interview Strategy: How to Approach This Problem

When you get "Design a Parking Lot" in an interview, follow this structured approach:

---

## Phase 1: Clarify Requirements (5-7 minutes)

### Questions to Ask Interviewer:

1. **Scope Clarification**
   - Single-level or multi-floor parking lot?
   - What vehicle types to support? (Bike, Car, Truck, Bus?)
   - Should we handle payments and pricing?
   - Is this for LLD (classes/code) or HLD (system architecture)?

2. **Vehicle and Spot Types**
   - How many vehicle types?
   - How to determine spot compatibility?
   - Can a small vehicle take a large spot?
   - Different pricing for different vehicle types?

3. **Parking Flow**
   - First-come-first-serve or pre-booking?
   - Nearest spot allocation or specific spot selection?
   - How to handle full parking lot?

4. **Concurrency Requirements**
   - Multiple vehicles parking simultaneously?
   - How to prevent double allocation of spots?
   - Thread safety required?

5. **Fee Calculation**
   - Hourly, daily, or flat rate?
   - Different rates for different vehicle types?
   - Special pricing (weekend, peak hours)?

6. **Scale Expectations**
   - How many floors and spots?
   - How many concurrent parking requests?
   - Real-time availability display?

### Expected Answer from Interviewer:
"Design a multi-floor parking lot supporting Bikes, Cars, and Trucks. Each floor has multiple spots of different sizes (Small, Medium, Large). Assign nearest available compatible spot. Prevent double allocation when multiple vehicles try to park simultaneously. Implement hourly fee calculation with different rates per vehicle type. Use in-memory storage."

---

## Phase 2: High-Level Design (5-10 minutes)

### Step 1: Identify Core Entities
Start by listing main entities:
- **Vehicle** (Type: Bike, Car, Truck)
- **ParkingSpot** (Size: Small, Medium, Large)
- **Floor** (Contains multiple spots)
- **ParkingLot** (Contains multiple floors)
- **Ticket** (Issued on entry)
- **FeeStrategy** (Calculates parking fee)

### Step 2: Define Key Features
Explain what each component does:
1. **Vehicle Entry** - Accept vehicle, find compatible spot, issue ticket
2. **Spot Allocation** - Find nearest available spot (floor-by-floor)
3. **Spot Compatibility** - Check if vehicle fits in spot
4. **Thread Safety** - Prevent double allocation
5. **Vehicle Exit** - Calculate fee, free spot
6. **Fee Calculation** - Pluggable pricing strategies

### Step 3: Identify Relationships
- ParkingLot → Floors (1:N)
- Floor → ParkingSpots (1:N)
- ParkingSpot → Vehicle (1:1 when occupied)
- Ticket → Vehicle + ParkingSpot (1:1)
- ParkingLot → FeeStrategy (1:1, pluggable)

### Step 4: Discuss Key Challenges

1. **Spot Compatibility** - Which vehicles fit in which spots?
   - Solution: Numeric size mapping (SMALL=1, MEDIUM=2, LARGE=3)
   - Rule: Vehicle can park if spot size ≥ vehicle size

2. **Thread Safety** - Multiple threads parking simultaneously
   - Solution: ReentrantLock for atomic check-and-reserve
   - Per-spot locking (fine-grained, not global)

3. **Nearest Spot Allocation** - Assign closest available spot
   - Solution: Check floors in order (1, 2, 3...)
   - Use queues for available spots by size

4. **Pluggable Fee Calculation** - Support different pricing strategies
   - Solution: Strategy Pattern
   - Easy to add new pricing models

**Draw a simple diagram on whiteboard:**
```
ParkingLot
    ↓ has
  Floors (1, 2, 3)
    ↓ has
  Spots (SMALL, MEDIUM, LARGE)
    ↓ occupied by
  Vehicle (BIKE, CAR, TRUCK)
    ↓ gets
  Ticket → exit → Fee
```

---

## Phase 3: Low-Level Design (10-15 minutes)

### Step 1: Define Enums First

**Start with enums (no dependencies):**
```java
enum VehicleType {
    BIKE(SpotSize.SMALL),
    CAR(SpotSize.MEDIUM),
    TRUCK(SpotSize.LARGE);

    private SpotSize requiredSize;
}

enum SpotSize {
    SMALL(1), MEDIUM(2), LARGE(3);

    boolean canFit(SpotSize vehicleSize) {
        return this.value >= vehicleSize.value;
    }
}

enum SpotStatus {
    AVAILABLE, OCCUPIED
}
```

### Step 2: Design Core Classes

**Vehicle (Simple):**
```java
class Vehicle {
    String licensePlate;
    VehicleType type;
}
```

**ParkingSpot (Critical - Thread-safe!):**
```java
class ParkingSpot {
    String spotId;
    SpotSize size;
    int floorNumber;
    SpotStatus status;
    Vehicle currentVehicle;
    ReentrantLock lock;  // ⭐ For thread safety

    boolean reserve(Vehicle vehicle) {
        lock.lock();
        try {
            if (status == AVAILABLE && canFit(vehicle)) {
                status = OCCUPIED;
                currentVehicle = vehicle;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}
```

**Floor (Queue-based allocation):**
```java
class Floor {
    int floorNumber;
    Map<String, ParkingSpot> allSpots;
    Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize;

    ParkingSpot getAvailableSpot(Vehicle vehicle) {
        // Try each spot size that can fit vehicle
        // Poll from queue and try atomic reservation
    }
}
```

**ParkingLot (Main system):**
```java
class ParkingLot {
    String parkingLotId;
    List<Floor> floors;
    Map<String, Ticket> activeTickets;
    FeeStrategy feeStrategy;

    Ticket parkVehicle(Vehicle vehicle);
    double exitVehicle(String ticketId);
}
```

### Step 3: Apply Design Patterns

**Mention patterns you'll use:**
1. **Strategy Pattern** - Fee calculation (Hourly, Weekend, Surge)
2. **Thread Safety** - ReentrantLock, ConcurrentHashMap, ConcurrentLinkedQueue
3. **Enum with Behavior** - SpotSize.canFit()
4. **Queue-based Allocation** - Fast spot lookup

**Example:**
```java
interface FeeStrategy {
    double calculateFee(Ticket ticket, Duration duration);
}

class HourlyFeeStrategy implements FeeStrategy {
    public double calculateFee(Ticket ticket, Duration duration) {
        long hours = duration.toHours();
        if (hours == 0) hours = 1;  // Minimum 1 hour

        double hourlyRate = getRate(ticket.getVehicle().getType());
        return hours * hourlyRate;
    }
}
```

---

## Phase 4: Implementation (25-35 minutes)

### Step 1: Start with Enums and Simple Models (10 min)

**Order of implementation:**
1. Enums (VehicleType, SpotSize, SpotStatus) - 3 min
2. Vehicle class - 2 min
3. Ticket class - 2 min
4. FeeStrategy interface + HourlyFeeStrategy - 3 min

**Pro Tip:** Write minimal code first, add details later if time permits.

### Step 2: Core Algorithm - Spot Compatibility (3 min)

**This is critical - implement in SpotSize enum:**
```java
public boolean canFit(SpotSize vehicleSize) {
    return this.value >= vehicleSize.value;
}
```

**Explain your logic out loud:**
"A spot can fit a vehicle if the spot size is greater than or equal to the vehicle's required size. SMALL(1) fits bikes, MEDIUM(2) fits bikes and cars, LARGE(3) fits all vehicles."

**Test mentally:**
- SMALL.canFit(BIKE) = 1 >= 1 = true ✓
- SMALL.canFit(CAR) = 1 >= 2 = false ✓
- LARGE.canFit(TRUCK) = 3 >= 3 = true ✓

### Step 3: Core Algorithm - Atomic Reservation (7 min)

**This is THE most important part:**
```java
public boolean reserve(Vehicle vehicle) {
    lock.lock();
    try {
        // Check availability and compatibility INSIDE lock
        if (status == SpotStatus.AVAILABLE && size.canFit(vehicle.getType().getRequiredSize())) {
            status = SpotStatus.OCCUPIED;
            currentVehicle = vehicle;
            return true;
        }
        return false;
    } finally {
        lock.unlock();  // Always unlock
    }
}
```

**Explain why this works:**
"Using ReentrantLock ensures the check-and-reserve operation is atomic. Only one thread can execute this block at a time. If two threads try to reserve the same spot, one gets the lock first, reserves it, and unlocks. The second thread then sees status is OCCUPIED and returns false. No double allocation possible."

### Step 4: Floor Class - Queue-based Allocation (8 min)

```java
public ParkingSpot getAvailableSpot(Vehicle vehicle) {
    SpotSize requiredSize = vehicle.getType().getRequiredSize();

    // Try each spot size (SMALL, MEDIUM, LARGE)
    for (SpotSize size : SpotSize.values()) {
        if (size.canFit(requiredSize)) {
            Queue<ParkingSpot> queue = availableSpotsBySize.get(size);

            ParkingSpot spot = queue.poll();
            while (spot != null) {
                // Try atomic reservation
                if (spot.reserve(vehicle)) {
                    return spot;  // Success!
                }
                // Failed (another thread got it) - try next
                spot = queue.poll();
            }
        }
    }
    return null;  // No compatible spot available
}
```

**Explain the algorithm:**
"I iterate through spot sizes in order. For each compatible size, I poll spots from the queue and try atomic reservation. If reservation fails (another thread got it), I try the next spot. This handles race conditions gracefully."

### Step 5: ParkingLot Class (5 min)

```java
public Ticket parkVehicle(Vehicle vehicle) {
    // Check floors in order (nearest first)
    for (Floor floor : floors) {
        ParkingSpot spot = floor.getAvailableSpot(vehicle);

        if (spot != null) {
            Ticket ticket = new Ticket(vehicle, spot);
            activeTickets.put(ticket.getId(), ticket);
            return ticket;
        }
    }
    return null;  // Parking FULL
}

public double exitVehicle(String ticketId) {
    Ticket ticket = activeTickets.remove(ticketId);
    if (ticket == null) throw new IllegalArgumentException();

    ticket.setExitTime(LocalDateTime.now());
    Duration duration = Duration.between(ticket.getEntryTime(), ticket.getExitTime());

    double fee = feeStrategy.calculateFee(ticket, duration);

    // Free the spot
    Floor floor = floors.get(ticket.getSpot().getFloorNumber() - 1);
    floor.freeSpot(ticket.getSpot());

    return fee;
}
```

### Step 6: Main.java Demo (2 min)

```java
public static void main(String[] args) {
    ParkingLot parkingLot = new ParkingLot("PL-001", 3);

    // Add spots to floors
    Floor floor1 = parkingLot.getFloor(1);
    floor1.addSpot(new ParkingSpot("F1-S1", SpotSize.SMALL, 1));
    floor1.addSpot(new ParkingSpot("F1-M1", SpotSize.MEDIUM, 1));

    // Park vehicles
    Vehicle bike = new Vehicle("BIKE-001", VehicleType.BIKE);
    Ticket t1 = parkingLot.parkVehicle(bike);

    // Exit and pay
    double fee = parkingLot.exitVehicle(t1.getTicketId());
    System.out.println("Fee: $" + fee);
}
```

---

## Phase 5: Testing & Discussion (5 minutes)

### Walk Through Your Code
1. "Vehicle arrives → System checks Floor 1 → Finds compatible spot → Tries atomic reservation → Success → Issues ticket"
2. "Two threads trying same spot → First thread locks, reserves, unlocks → Second thread locks, sees occupied, unlocks → Second thread tries next spot"

### Discuss Edge Cases
- **Parking full?** → parkVehicle() returns null
- **Invalid ticket?** → Throw IllegalArgumentException
- **Concurrent parking?** → Atomic reservation prevents double allocation
- **Wrong vehicle type?** → Compatibility check rejects

### Discuss Improvements
If interviewer asks "How would you improve this?"
- **Distributed locking** (Redis) for multiple parking lot instances
- **Database persistence** instead of in-memory maps
- **Pre-booking** with reservation system
- **Real-time display** showing available spots per floor
- **Analytics** (occupancy rate, revenue, popular times)
- **Dynamic pricing** based on demand
- **IoT integration** with sensors for automatic detection
- **Mobile app** with QR code ticket scanning

---

## Common Mistakes to Avoid

❌ **Don't:**
1. Forget thread safety (critical requirement!)
2. Use global lock (kills concurrency)
3. Ignore compatibility logic
4. Hardcode fee calculation (use Strategy Pattern)
5. Forget to free spots on exit
6. Overcomplicate with unnecessary features
7. Skip the working demo

✅ **Do:**
1. Use ReentrantLock for atomic operations
2. Implement per-spot locking (fine-grained)
3. Show compatibility logic clearly (SpotSize.canFit())
4. Apply Strategy Pattern for fees
5. Handle all edge cases gracefully
6. Write clean, testable code
7. Demonstrate with concurrent parking test

---

## Time Management (60-minute interview)

| Phase | Time | Activities |
|-------|------|------------|
| **Requirements** | 5-7 min | Ask questions, clarify scope |
| **HLD** | 5-10 min | Entities, relationships, key challenges |
| **LLD** | 10-15 min | Class design, thread safety strategy |
| **Coding** | 25-35 min | Enums, models, services, Main.java |
| **Testing** | 5 min | Walk through code, discuss concurrency |

---

## Key Talking Points During Interview

1. **When designing SpotSize:**
   "I'm using numeric values (1, 2, 3) to make compatibility checks simple. A spot can fit a vehicle if spot size >= vehicle size. This is O(1) constant-time check."

2. **When implementing reserve():**
   "The critical piece here is atomic reservation. I'm using ReentrantLock to ensure check-and-reserve happens atomically. This prevents the classic race condition where two threads see the same spot as available and both try to take it."

3. **When designing Floor:**
   "I'm using ConcurrentLinkedQueue for thread-safe spot management. When a spot is freed, I offer it back to the queue. When allocating, I poll and try reservation. If reservation fails due to a race condition, I simply try the next spot."

4. **When implementing parkVehicle():**
   "I check floors in order to assign the nearest spot. This is floor-by-floor allocation. On each floor, I delegate to the floor's getAvailableSpot() method which handles the queue-based search and atomic reservation."

5. **When discussing Strategy Pattern:**
   "I'm using Strategy Pattern for fee calculation. This makes it easy to add new pricing strategies like weekend pricing, surge pricing, or monthly passes without modifying the ParkingLot class. This follows Open/Closed Principle."

6. **When asked about scalability:**
   "For scale, we'd need:
   - Distributed locking with Redis
   - Database persistence with transactions
   - Event-driven architecture for analytics
   - Caching layer for floor availability
   - Horizontal scaling with multiple parking lot instances"

---

## What Interviewers Look For

✅ **Strong Candidates:**
- Immediately identify thread safety as critical requirement
- Design atomic reservation with clear explanation
- Implement compatibility logic correctly
- Apply Strategy Pattern appropriately
- Handle race conditions gracefully
- Write working concurrent parking test
- Explain trade-offs clearly

❌ **Weak Candidates:**
- Ignore thread safety completely
- Use synchronized(this) or global locks
- Can't explain why atomic operation is needed
- Hardcode fee calculation
- Don't test concurrent parking
- Can't handle edge cases

---

## Summary

**Your approach should be:**
1. ✅ Clarify → 2. ✅ Design (HLD + LLD) → 3. ✅ Implement → 4. ✅ Test → 5. ✅ Discuss

**Focus on:**
- Thread safety (ReentrantLock for atomic reservation)
- Compatibility logic (SpotSize.canFit())
- Queue-based allocation (ConcurrentLinkedQueue)
- Strategy Pattern (pluggable fee calculation)
- Working concurrent test

**The Three Must-Memorize Pieces:**
1. **Compatibility**: `return this.value >= vehicleSize.value`
2. **Atomic Reservation**: lock → check → reserve → unlock
3. **Strategy Pattern**: Pluggable FeeStrategy interface

**Remember:**
- Thread safety is THE key challenge
- Think out loud about concurrency
- Demonstrate with concurrent parking test
- Explain why atomic operations prevent double allocation

---

**Now proceed to the actual Java implementation in the following files!**
