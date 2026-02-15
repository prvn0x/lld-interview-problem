# 🎯 PARKING LOT - INTERVIEW GUIDE

## What You Need to Memorize & Show

---

## 📁 Structure

```
parkinglot/
├── enums/
│   ├── VehicleType.java      # BIKE, CAR, TRUCK
│   ├── SpotSize.java          # SMALL, MEDIUM, LARGE
│   └── SpotStatus.java        # AVAILABLE, OCCUPIED
├── models/
│   ├── Vehicle.java
│   ├── ParkingSpot.java       ⭐ Thread-safe with ReentrantLock
│   ├── Ticket.java
│   ├── Floor.java             ⭐ Queue-based allocation
│   └── ParkingLot.java        ⭐ Main system
├── strategies/
│   ├── FeeStrategy.java       ⭐ Strategy Pattern interface
│   ├── HourlyFeeStrategy.java
│   └── WeekendFeeStrategy.java
└── Main.java                   ✅ RUN THIS
```

---

## 🧠 MEMORIZE These 3 Things

### 1️⃣ Spot Compatibility Logic

```java
public boolean canFit(SpotSize vehicleSize) {
    return this.value >= vehicleSize.value;
}

// SMALL (1) can fit: BIKE
// MEDIUM (2) can fit: BIKE, CAR
// LARGE (3) can fit: BIKE, CAR, TRUCK
```

**Say:** "Vehicle can only park if spot size >= vehicle size. Bike can fit anywhere, Car needs Medium/Large, Truck needs only Large."

### 2️⃣ Atomic Reservation (Thread-Safe!)

```java
public boolean reserve(Vehicle vehicle) {
    lock.lock();  // ReentrantLock
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
```

**Say:** "Using ReentrantLock for atomic spot reservation. Prevents double allocation when multiple threads try to park simultaneously."

### 3️⃣ Strategy Pattern for Fee Calculation

```java
interface FeeStrategy {
    double calculateFee(Ticket ticket, Duration duration);
}

// Usage
parkingLot.setFeeStrategy(new HourlyFeeStrategy());
// or
parkingLot.setFeeStrategy(new WeekendFeeStrategy());
```

**Say:** "Strategy Pattern makes fee calculation pluggable. Can easily add new strategies like surge pricing or flat rate without modifying ParkingLot code."

---

## 🎬 Interview Flow

### PHASE 1: Clarify (5 min)

**Ask:**
- "How many vehicle types?" (3: Bike, Car, Truck)
- "Spot size rules?" (Small/Medium/Large, compatibility)
- "How to assign spots?" (Nearest available = check floors in order)
- "Multiple floors?" (Yes)
- "Thread safety needed?" (Yes - prevent double allocation)
- "Fee calculation?" (Pluggable - Strategy Pattern)

---

### PHASE 2: High-Level Design (10 min)

**Draw:**
```
ParkingLot
  ↓ has
Floor (multiple)
  ↓ has
ParkingSpot (multiple, by size)
  ↓ occupied by
Vehicle
  ↓ gets
Ticket
```

**Key challenges:**

**1. Nearest Spot Allocation**
- Check floors in order (1, 2, 3...)
- On each floor, check spot size queues
- Return first compatible available spot

**2. Thread Safety**
- Use ReentrantLock in ParkingSpot
- Atomic check-and-reserve operation
- ConcurrentLinkedQueue for spot queues

**3. Pluggable Fee Calculation**
- Strategy Pattern
- Different rates: hourly, weekend, surge

---

### PHASE 3: Class Design (15 min)

**Core classes:**

```java
// 1. VehicleType enum
enum VehicleType {
    BIKE(SpotSize.SMALL),
    CAR(SpotSize.MEDIUM),
    TRUCK(SpotSize.LARGE);
}

// 2. SpotSize enum with compatibility
enum SpotSize {
    SMALL(1), MEDIUM(2), LARGE(3);

    boolean canFit(SpotSize vehicleSize) {
        return this.value >= vehicleSize.value;
    }
}

// 3. ParkingSpot with thread-safe reservation
class ParkingSpot {
    ReentrantLock lock;

    boolean reserve(Vehicle vehicle) {
        lock.lock();
        try {
            if (available && canFit(vehicle)) {
                occupy(vehicle);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }
}

// 4. Floor with queue-based allocation
class Floor {
    Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize;

    ParkingSpot getAvailableSpot(Vehicle vehicle) {
        // Try each spot size in order
        for (SpotSize size : SpotSize.values()) {
            if (size.canFit(vehicle.getSize())) {
                ParkingSpot spot = queue.get(size).poll();
                if (spot != null && spot.reserve(vehicle)) {
                    return spot;
                }
            }
        }
        return null;
    }
}

// 5. ParkingLot with floor-by-floor allocation
class ParkingLot {
    List<Floor> floors;
    FeeStrategy feeStrategy;

    Ticket parkVehicle(Vehicle vehicle) {
        for (Floor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(vehicle);
            if (spot != null) {
                return new Ticket(vehicle, spot);
            }
        }
        return null;  // Full
    }
}
```

---

### PHASE 4: Implementation (30 min)

**Order:**
1. Enums (5 min) - VehicleType, SpotSize, SpotStatus
2. Strategy (5 min) - FeeStrategy interface + HourlyFeeStrategy
3. Vehicle + Ticket (3 min) - Simple POJOs
4. **ParkingSpot** (7 min) - ⭐ Thread-safe with lock
5. **Floor** (7 min) - Queue-based allocation
6. **ParkingLot** (5 min) - Main system
7. Main.java demo (3 min)

---

## 🎯 Key Talking Points

### When Discussing Thread Safety:

**You:** "I'm using ReentrantLock in ParkingSpot for atomic reservation. When multiple threads try to park simultaneously, the lock ensures only one succeeds in reserving each spot. The check-and-reserve operation happens atomically inside the lock, preventing double allocation."

### When Discussing Spot Allocation:

**You:** "I check floors in order (nearest first). On each floor, I try spot sizes from smallest to largest that can fit the vehicle. This ensures bikes don't take up large spots unnecessarily. I use ConcurrentLinkedQueue for thread-safe queue operations."

### When Discussing Strategy Pattern:

**You:** "I implemented Strategy Pattern for fee calculation. The interface has one method: calculateFee(). Different strategies like HourlyFeeStrategy and WeekendFeeStrategy implement this interface. The ParkingLot class uses the strategy without knowing implementation details. This makes it easy to add new pricing models like surge pricing or monthly passes."

### When Asked About Scalability:

**You:** "For scale, we'd:
1. Distribute parking lots geographically
2. Use distributed locking (Redis) instead of ReentrantLock
3. Event-driven architecture for entry/exit
4. Database per parking lot for isolation
5. Cache floor availability in Redis"

---

## 📝 Quick Reference Card

```
┌────────────────────────────────────────┐
│ PARKING LOT CHEAT SHEET                │
├────────────────────────────────────────┤
│ 1. Compatibility:                      │
│    Spot size >= Vehicle size           │
│    SMALL(1) ≤ MEDIUM(2) ≤ LARGE(3)    │
│                                        │
│ 2. Thread Safety:                      │
│    ReentrantLock for atomic reserve    │
│                                        │
│ 3. Allocation:                         │
│    Floor-by-floor, size-by-size        │
│                                        │
│ 4. Strategy Pattern:                   │
│    Pluggable fee calculation           │
│                                        │
│ 5. Concurrency:                        │
│    ConcurrentLinkedQueue + locks       │
└────────────────────────────────────────┘
```

---

## ✅ Pre-Interview Checklist

**Can you explain:**
- [ ] Spot compatibility logic (size comparison)
- [ ] Atomic reservation with ReentrantLock
- [ ] Strategy Pattern for fee calculation
- [ ] Queue-based allocation on each floor
- [ ] How to prevent double allocation

**Can you code:**
- [ ] SpotSize.canFit() method
- [ ] ParkingSpot.reserve() with lock
- [ ] Floor.getAvailableSpot() logic
- [ ] FeeStrategy interface

---

## 🚀 To Run:

```bash
cd lld-interview-problems
javac -d out -sourcepath src src/com/lld/parkinglot/Main.java
java -cp out com.lld.parkinglot.Main
```

---

## 💪 Success = Show You Can:

1. ✅ **Design clean class hierarchy** (Vehicle, Spot, Floor, ParkingLot)
2. ✅ **Implement thread-safe spot reservation** (ReentrantLock)
3. ✅ **Apply Strategy Pattern** (pluggable fee calculation)
4. ✅ **Handle concurrency** (no double allocation)
5. ✅ **Allocate nearest spot** (floor-by-floor search)

---

## 🎯 Common Questions & Answers

**Q: How do you prevent two threads from getting the same spot?**
A: "I use ReentrantLock in ParkingSpot. The reserve() method is synchronized using lock/unlock. Only one thread can execute the check-and-reserve logic at a time, ensuring atomic operation."

**Q: Why Strategy Pattern for fees?**
A: "It makes pricing logic pluggable. We can add new strategies without modifying ParkingLot code (Open/Closed Principle). Easy to A/B test different pricing models."

**Q: Why queue for spots?**
A: "ConcurrentLinkedQueue allows thread-safe operations. When a floor needs a spot, it polls the queue. When spot freed, we offer it back. Queue order doesn't matter much since any available spot works."

**Q: What if parking lot is full?**
A: "parkVehicle() returns null. Caller handles gracefully - maybe show 'FULL' sign or suggest nearby parking lots."

---

## 🎓 Design Patterns Used

1. **Strategy Pattern** - FeeStrategy
2. **Thread Safety** - ReentrantLock, ConcurrentLinkedQueue
3. **Enum with behavior** - SpotSize.canFit()

---

**YOU'RE READY!** 🚀

Just explain: Compatibility + Thread Safety + Strategy Pattern

Good luck! 💪
