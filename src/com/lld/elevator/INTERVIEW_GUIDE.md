# 🎯 ELEVATOR SYSTEM - INTERVIEW GUIDE

## What You Need to Memorize & Show

---

## 📁 Structure

```
elevator/
├── enums/
│   ├── Direction.java         # UP, DOWN, IDLE
│   └── ElevatorState.java     # IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
├── models/
│   ├── Request.java           # Request data
│   ├── Elevator.java          ⭐ Core logic with FSM & dual queues
│   └── Building.java          # Public APIs
├── services/
│   └── ElevatorController.java ⭐ Scoring-based dispatcher
└── Main.java                   ✅ RUN THIS
```

---

## 🧠 MEMORIZE These 3 Things

### 1️⃣ Directional Scheduling (Dual Queue Logic)

```java
// When moving UP
if (floor > currentFloor) {
    upStops.add(floor);  // Add to current direction queue
} else {
    downStops.add(floor);  // Add to opposite direction queue
}

// When moving DOWN
if (floor < currentFloor) {
    downStops.add(floor);  // Add to current direction queue
} else {
    upStops.add(floor);  // Add to opposite direction queue
}
```

**Say:** "Elevator maintains separate queues for UP and DOWN. Requests are added to the appropriate queue based on current direction and position. This ensures efficient directional scheduling."

### 2️⃣ Scoring-Based Dispatcher

```java
int score = distance + directionPenalty + loadPenalty;

// Distance: abs(currentFloor - requestFloor)
// Direction Penalty:
//   - 0: moving toward request
//   - 5: idle
//   - 20: moving away from request
// Load Penalty: number of pending stops

// Select elevator with MINIMUM score
```

**Say:** "Dispatcher uses scoring algorithm considering distance, direction alignment, and load. Lower score = better fit. This ensures optimal elevator assignment."

### 3️⃣ Atomic Reservation with ReentrantLock

```java
public void addStop(int floor) {
    lock.lock();
    try {
        // Add to appropriate queue
        if (direction == Direction.UP && floor > currentFloor) {
            upStops.add(floor);
        } else if (direction == Direction.DOWN && floor < currentFloor) {
            downStops.add(floor);
        }
        // ... rest of logic
    } finally {
        lock.unlock();
    }
}
```

**Say:** "Using ReentrantLock for thread safety. Each elevator has its own lock (fine-grained locking). Multiple elevators can be modified concurrently without blocking each other."

---

## 🎬 Interview Flow

### PHASE 1: Clarify (5 min)

**Ask:**
- "How many elevators and floors?" (Multiple elevators, 10+ floors)
- "External requests with direction or just call?" (With direction: UP/DOWN)
- "Internal floor selection?" (Yes, from inside elevator)
- "Scheduling algorithm?" (Directional scheduling with separate queues)
- "Thread safety needed?" (Yes - concurrent requests)
- "Dispatcher logic?" (Scoring-based: distance + direction + load)

---

### PHASE 2: High-Level Design (10 min)

**Draw:**
```
Building
  ↓ has
Elevators (multiple)
  ↓ uses
ElevatorController (Dispatcher)
  ↓ selects best
Elevator
  ↓ has
State Machine (IDLE/MOVING/DOOR_OPEN)
  ↓ has
Dual Queues (upStops, downStops)
```

**Key challenges:**

**1. Directional Scheduling**
- Separate queues for UP and DOWN
- Serve stops in direction of movement
- Add requests to appropriate queue based on position

**2. Dispatcher Logic**
- Score each elevator for request
- Consider distance, direction, load
- Select minimum score

**3. Thread Safety**
- Per-elevator ReentrantLock
- Thread-safe queues (TreeSet)
- Atomic state modifications

**4. State Machine**
- IDLE → MOVING → DOOR_OPEN → back to MOVING or IDLE
- Direction changes only when queue empty

---

### PHASE 3: Class Design (15 min)

**Core classes:**

```java
// 1. Direction enum
enum Direction {
    UP, DOWN, IDLE
}

// 2. ElevatorState enum
enum ElevatorState {
    IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
}

// 3. Elevator with dual queues
class Elevator {
    int id, currentFloor;
    Direction direction;
    ElevatorState state;

    Set<Integer> upStops;    // TreeSet (ascending)
    Set<Integer> downStops;  // TreeSet (descending)
    ReentrantLock lock;

    void addStop(int floor);
    void moveOneStep();
    void processArrival();
    void changeDirectionIfNeeded();
    int calculateScore(int requestFloor, Direction direction);
}

// 4. ElevatorController (Dispatcher)
class ElevatorController {
    Elevator selectBestElevator(List<Elevator> elevators,
                                int requestFloor,
                                Direction direction) {
        // Calculate score for each elevator
        // Return elevator with minimum score
    }
}

// 5. Building with public APIs
class Building {
    List<Elevator> elevators;
    ElevatorController controller;

    void requestElevator(int floor, Direction direction);
    void selectFloor(int elevatorId, int destination);
    void simulateStep();
}
```

---

### PHASE 4: Implementation (30 min)

**Order:**
1. Enums (3 min) - Direction, ElevatorState
2. Request class (2 min) - Simple POJO
3. **Elevator** (15 min) - ⭐ Core logic with FSM and dual queues
4. **ElevatorController** (5 min) - Scoring algorithm
5. **Building** (3 min) - Public APIs
6. Main.java demo (2 min)

---

## 🎯 Key Talking Points

### When Discussing Directional Scheduling:

**You:** "The elevator maintains two separate queues: upStops and downStops. When moving UP, it serves all upStops in ascending order. When a new request comes, it's added to upStops if the floor is ahead in the current direction, otherwise to downStops. This ensures all passengers going in the same direction are served efficiently."

### When Discussing Scoring Algorithm:

**You:** "The dispatcher calculates a score for each elevator considering three factors:
1. Distance: How far is the elevator from request floor
2. Direction penalty: 0 if moving toward, 5 if idle, 20 if moving away
3. Load penalty: Number of pending stops

The elevator with minimum score is selected. This balances proximity, direction alignment, and load distribution."

### When Discussing State Machine:

**You:** "The elevator follows a finite state machine:
- IDLE: No pending requests
- MOVING_UP/DOWN: Traveling between floors
- DOOR_OPEN: At a stop with door open

Transitions happen at floor arrivals. The elevator continues in the same direction until the queue for that direction is empty, then reverses or becomes idle."

### When Discussing Thread Safety:

**You:** "Each elevator has its own ReentrantLock for fine-grained concurrency. This allows multiple elevators to be modified simultaneously without blocking each other. The stop queues use TreeSet which maintains sorted order. All state modifications happen inside locks ensuring atomic operations."

### When Asked About Scalability:

**You:** "For scale, we'd:
1. Database persistence for state recovery
2. Event-driven architecture with message queues
3. Distributed system with elevator groups
4. Real-time monitoring and analytics
5. Predictive dispatch using ML (learn traffic patterns)"

---

## 📝 Quick Reference Card

```
┌────────────────────────────────────────┐
│ ELEVATOR SYSTEM CHEAT SHEET            │
├────────────────────────────────────────┤
│ 1. Dual Queues:                        │
│    upStops (ascending)                 │
│    downStops (descending)              │
│                                        │
│ 2. Scheduling Rule:                    │
│    Add to current queue if ahead       │
│    Else add to opposite queue          │
│                                        │
│ 3. Scoring:                            │
│    score = distance + dirPenalty       │
│            + loadPenalty               │
│    Select MIN score                    │
│                                        │
│ 4. State Machine:                      │
│    IDLE ↔ MOVING ↔ DOOR_OPEN          │
│                                        │
│ 5. Thread Safety:                      │
│    ReentrantLock per elevator          │
│    TreeSet for ordered queues          │
└────────────────────────────────────────┘
```

---

## ✅ Pre-Interview Checklist

**Can you explain:**
- [ ] Directional scheduling with dual queues
- [ ] Scoring-based dispatcher algorithm
- [ ] State machine transitions
- [ ] Thread safety with ReentrantLock
- [ ] How to prevent race conditions

**Can you code:**
- [ ] addStop() method with queue logic
- [ ] calculateScore() method
- [ ] moveOneStep() with state transitions
- [ ] selectBestElevator() dispatcher logic

---

## 🚀 To Run:

```bash
cd lld-interview-problems
javac -d out -sourcepath src src/com/lld/elevator/Main.java
java -cp out com.lld.elevator.Main
```

---

## 💪 Success = Show You Can:

1. ✅ **Design dual-queue directional scheduling**
2. ✅ **Implement scoring-based dispatcher**
3. ✅ **Build state machine with proper transitions**
4. ✅ **Handle thread safety with fine-grained locking**
5. ✅ **Balance load across multiple elevators**

---

## 🎯 Common Questions & Answers

**Q: Why two queues instead of one sorted queue?**
A: "Two queues allow efficient directional scheduling. When moving UP, we serve all upward requests without unnecessary direction changes. This minimizes passenger wait time and improves throughput."

**Q: How do you prevent two elevators from serving the same request?**
A: "The dispatcher assigns ONE best elevator per request. That elevator adds the floor to its queue. There's no global request queue that multiple elevators compete for."

**Q: What if an elevator is full?**
A: "Currently, we don't track capacity. In production, we'd add maxCapacity field and include it in scoring. Full elevators would get a high penalty or be skipped."

**Q: Why ReentrantLock instead of synchronized?**
A: "ReentrantLock is more flexible - explicit lock/unlock, can try-lock with timeout, better performance under contention. Each elevator has its own lock enabling true concurrent operations."

**Q: How to handle emergency stop?**
A: "Add EMERGENCY state to state machine. Clear all queues, open door, disable normal operations. Requires admin intervention to reset."

---

## 🎓 Design Patterns Used

1. **State Machine Pattern** - ElevatorState transitions
2. **Strategy Pattern** - Scoring-based dispatcher
3. **Thread Safety** - ReentrantLock, TreeSet
4. **Queue-based Scheduling** - Dual directional queues

---

## 🧮 Time Complexity

| Operation | Time Complexity |
|-----------|----------------|
| addStop() | O(log n) - TreeSet insertion |
| calculateScore() | O(1) |
| selectBestElevator() | O(m) where m = elevators |
| moveOneStep() | O(log n) - TreeSet removal |
| processArrival() | O(log n) |

---

**YOU'RE READY!** 🚀

**Remember:**
- Dual queues for directional scheduling
- Scoring = distance + direction + load
- State machine for behavior
- Per-elevator locks for thread safety

Good luck! 💪
