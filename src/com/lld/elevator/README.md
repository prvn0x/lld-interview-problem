# Elevator System - Implementation Guide

## 📋 Problem Statement

Design an elevator system for a multi-floor building where:
- Multiple elevators serve a building with configurable floors
- Users can request elevators from any floor (external requests with direction)
- Users inside elevators can select destination floors (internal selections)
- System uses intelligent scoring to dispatch best elevator to each request
- Elevators use directional scheduling with separate UP/DOWN queues
- All operations are thread-safe for concurrent requests
- Clear state machine governs elevator behavior

---

## 🎯 Features Implemented

### ✅ Core Features

1. **Multi-Floor Building Support**
   - Configurable number of floors
   - Elevators start at ground floor (0)
   - Valid floor range: 0 to (totalFloors - 1)
   - Scalable to 100+ floors

2. **Multiple Elevators**
   - Any number of elevators per building
   - Each elevator independent with own state
   - Parallel operation without blocking
   - Distributed load across elevators

3. **External Request Handling**
   - Users request elevator from any floor with direction (UP/DOWN)
   - System validates floor and direction
   - Dispatcher selects best available elevator
   - Request assigned to optimal elevator

4. **Internal Floor Selection**
   - Users inside elevator select destination floor
   - Internal selection just adds to elevator's queue
   - No dispatcher involvement
   - Direct addition to appropriate queue

5. **Directional Scheduling (Dual Queues)**
   - Separate `upStops` for UP direction (ascending order)
   - Separate `downStops` for DOWN direction (descending order)
   - Elevator serves all stops in one direction before reversing
   - Minimizes direction changes
   - Reduces total travel time

6. **Intelligent Dispatch Algorithm**
   - Scoring-based selection: `Score = Distance + DirectionPenalty + LoadPenalty`
   - Considers: proximity, direction compatibility, current load
   - Automatically distributes load across elevators
   - Prevents overloading single elevator

7. **State Machine Behavior**
   - **IDLE**: No pending requests, not moving
   - **MOVING_UP**: Moving toward higher floors
   - **MOVING_DOWN**: Moving toward lower floors
   - **DOOR_OPEN**: Stopped at floor with open door
   - Clear transitions between states

8. **Thread-Safe Concurrency**
   - Per-elevator ReentrantLock (fine-grained)
   - Multiple threads can request simultaneously
   - No global bottleneck
   - Atomic state updates
   - Safe concurrent queue modifications

9. **Demonstration Features**
   - 8-step comprehensive demo in Main.java
   - Shows single requests, multiple requests, concurrent requests
   - Displays elevator status and movement
   - Test edge cases (invalid floor, same floor, etc.)
   - Shows dispatcher scoring and selection

---

## 🏗️ Architecture

### Package Structure

```
com.lld.elevator/
├── enums/
│   ├── Direction.java           # UP, DOWN, IDLE
│   └── ElevatorState.java       # IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
├── models/
│   ├── Building.java            # System coordinator
│   ├── Elevator.java            # ⭐ Individual elevator with dual queues
│   └── Request.java             # (Optional) Request metadata
├── services/
│   └── ElevatorController.java  # ⭐ Scoring-based dispatcher
└── Main.java                     # ✅ RUN THIS - Comprehensive demo
```

### Class Responsibilities

| Class | Responsibility |
|-------|-----------------|
| **Building** | Coordinate elevators, accept requests, manage simulation |
| **Elevator** | Manage state, maintain stop queues, handle movement |
| **ElevatorController** | Select best elevator using scoring algorithm |
| **Direction** | Enum for direction values (UP, DOWN, IDLE) |
| **ElevatorState** | Enum for state machine states |

### Design Patterns Applied

1. **State Machine**: Clear states and transitions for elevator
2. **Strategy Pattern**: Scoring algorithm for dispatcher
3. **Thread Safety Pattern**: Per-object locking with ReentrantLock
4. **Facade Pattern**: Building provides simple interface
5. **Enum with Behavior**: Direction and ElevatorState enums

---

## 🚀 How to Run

### Using IntelliJ IDEA (Recommended)

1. Open project in IntelliJ
2. Navigate to `src/com/lld/elevator/Main.java`
3. Right-click → **Run 'Main.main()'**
4. See comprehensive demo output with elevator movements

### Using Command Line

```bash
# From project root
cd /Users/praveen.singh/Desktop/lld-interview-problems

# Compile all elevator classes
javac -d out -sourcepath src src/com/lld/elevator/Main.java

# Run
java -cp out com.lld.elevator.Main
```

### Using Maven (If Available)

```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.lld.elevator.Main"
```

---

## 📊 Demo Flow Explanation

The `Main.java` demonstrates complete system functionality through 8 steps:

### Step 1: Initialize Building
```
Tower-A: 10 floors, 3 elevators
All elevators start at floor 0
Display: Building status
```

### Step 2: Single External Request
```
Request: Floor 5, Direction UP
Dispatcher evaluates all elevators
Selects best elevator
Elevator moves: 0→1→2→3→4→5
Door opens at floor 5
Expected: Single elevator handles request
```

### Step 3: Multiple Requests - Directional Scheduling
```
External request: Floor 2, UP
Internal selections: Floors 5, 7, 3

Expected behavior:
- Elevator picks up floor 2 (external)
- Moves UP: 2→3→5→7 (all UP direction)
- Both queues empty
- reverses to DOWN: 7→...→3 (DOWN direction)

Shows: Efficient dual-queue scheduling
```

### Step 4: Scoring-Based Dispatcher
```
Position elevators at different floors:
- Elevator-1 at floor 2
- Elevator-2 at floor 5
- Elevator-3 at floor 8

Request: Floor 6, Direction UP

Scoring:
- Elevator-1: |2-6| + 0 + load = high score
- Elevator-2: |5-6| + 0 + load = low score ← Selected
- Elevator-3: |8-6| + 20 + load = high score

Shows: Dispatcher picks closest, direction-compatible elevator
```

### Step 5: Direction Penalty Test
```
Elevator-1 moving UP, already passed floor 3
Request: Floor 3, Direction UP

Scoring:
- Elevator-1 moving away: penalty = 20
- Elevator-2 available: no penalty = 0 ← Selected

Shows: Algorithm penalizes wrong direction
```

### Step 6: Load Balancing
```
Elevator-1 has 5 pending stops (heavily loaded)
Request: Floor 2

Scoring:
- Elevator-1: 2 + 0 + 5 = 7
- Elevator-2: 2 + 0 + 0 = 2 ← Selected
- Elevator-3: 2 + 0 + 1 = 3

Shows: Load penalty distributes requests evenly
```

### Step 7: Concurrent Requests (Thread Safety)
```
5 threads simultaneously request elevators:
- Thread-1: requestElevator(1, UP)
- Thread-2: requestElevator(4, DOWN)
- Thread-3: requestElevator(7, UP)
- Thread-4: requestElevator(3, DOWN)
- Thread-5: requestElevator(9, DOWN)

All threads: lock.join() waits for completion

Shows: Thread-safe concurrent handling
No race conditions, consistent state
```

### Step 8: Edge Cases
```
1. Invalid floor (20): Validation rejects
2. IDLE direction: Validation rejects
3. Same floor request: Validation skips
```

---

## 🔑 Key Algorithms Explained

### 1. Directional Scheduling (Dual Queue Strategy)

**Problem**: How to minimize elevator direction changes?

**Solution**: Separate queues for each direction

**Example**:
```
Requests: [2, 5, 3, 7, 4]
Elevator at floor 0, moving UP

Without dual queues:
Queue: [2, 5, 3, 7, 4]
Movement: 0→2→3→4→5→7→(back to 3) = inefficient

With dual queues:
upStops: [2, 3, 4, 5, 7]     (ascending)
downStops: []
Movement: 0→2→3→4→5→7 = efficient!
```

**Implementation**:
```java
// TreeSet maintains sorted order automatically
Set<Integer> upStops = new TreeSet<>();      // [1, 3, 5]
Set<Integer> downStops = new TreeSet<>((a,b) -> b.compareTo(a));  // [9, 7, 5]
```

**addStop() Logic**:
```java
if (direction == Direction.UP) {
    if (floor > currentFloor) upStops.add(floor);    // Going up
    else downStops.add(floor);                        // Will visit on reverse
} else if (direction == Direction.DOWN) {
    if (floor < currentFloor) downStops.add(floor);  // Going down
    else upStops.add(floor);                          // Will visit on reverse
} else {  // IDLE
    if (floor > currentFloor) upStops.add(floor);
    else downStops.add(floor);
}
```

### 2. Scoring-Based Dispatcher

**Problem**: Which elevator should serve this request?

**Solution**: Calculate score for each elevator, select minimum

**Scoring Formula**:
```
Score = Distance + DirectionPenalty + LoadPenalty

Where:
Distance = abs(elevatorFloor - requestFloor)

DirectionPenalty:
  - 5 if IDLE (can go anywhere)
  - 0 if moving toward request (on the way)
  - 20 if moving away or opposite direction

LoadPenalty = count of pending stops
```

**Example Calculation**:
```
Request: Floor 6, Direction UP

Elevator-1:
  Position: floor 2, direction UP, stops: 2
  Distance = |2 - 6| = 4
  Direction = 0 (moving up, floor 6 is ahead)
  Load = 2
  Score = 4 + 0 + 2 = 6

Elevator-2:
  Position: floor 5, direction UP, stops: 2
  Distance = |5 - 6| = 1
  Direction = 0 (moving up, floor 6 is ahead)
  Load = 2
  Score = 1 + 0 + 2 = 3 ← SELECTED (minimum)

Elevator-3:
  Position: floor 8, direction DOWN, stops: 2
  Distance = |8 - 6| = 2
  Direction = 20 (moving away)
  Load = 2
  Score = 2 + 20 + 2 = 24
```

**Benefits**:
- Considers multiple factors
- Automatically load-balances
- Prefers direction-compatible elevators
- Fair distribution across all elevators

### 3. State Machine Transitions

**States**:
```
IDLE ← No pending stops
  ↓ (addStop called, changeDirectionIfNeeded)
MOVING_UP / MOVING_DOWN ← Have stops in direction
  ↓ (reached floor, processArrival)
DOOR_OPEN ← At a stop
  ↓ (next moveOneStep)
MOVING_UP / MOVING_DOWN / IDLE ← changeDirectionIfNeeded logic
```

**changeDirectionIfNeeded() Logic**:
```
IF direction == UP (or IDLE):
  IF upStops not empty → MOVING_UP
  ELSE IF downStops not empty → MOVING_DOWN (reverse)
  ELSE → IDLE

IF direction == DOWN:
  IF downStops not empty → MOVING_DOWN
  ELSE IF upStops not empty → MOVING_UP (reverse)
  ELSE → IDLE
```

**Example Flow**:
```
Initial: IDLE, upStops=[], downStops=[]
Request floor 5: IDLE → addStop(5) → upStops=[5] → changeDirectionIfNeeded
Result: MOVING_UP

At floor 5: MOVING_UP → processArrival → DOOR_OPEN
At floor 5: DOOR_OPEN → closeDoor → changeDirectionIfNeeded
Result: upStops empty, downStops empty → IDLE
```

---

## 🔒 Thread Safety Deep-Dive

### Problem: Race Conditions in Concurrent Requests

```
Thread-1: elevator.addStop(5)
Thread-2: elevator.addStop(7)
Thread-3: elevator.moveOneStep()

Without locking:
T1: Check direction
T2: Check direction
T1: Add 5 to upStops
T2: Add 7 to upStops  ← What if upStops is not thread-safe?
T3: moveOneStep()     ← What if state is partially updated?
```

### Solution: Per-Elevator ReentrantLock

```java
private final ReentrantLock lock = new ReentrantLock();

public void addStop(int floor) {
    lock.lock();  // Only one thread at a time
    try {
        // All state modifications here are protected
        // No other thread can access until unlock
        if (floor == currentFloor) return;

        if (direction == Direction.UP) {
            if (floor > currentFloor) {
                upStops.add(floor);  // Safe!
            } else {
                downStops.add(floor);
            }
        }
        // ... more logic ...
    } finally {
        lock.unlock();  // Always release lock
    }
}
```

### Why Per-Elevator? (Not Global)

**Global Lock (❌ Bad)**:
```java
synchronized(building) {  // One lock for all elevators
    elevator1.addStop(5);
    // Only one thread can modify ANY elevator
    // If 10 threads request, 9 wait for 1
}
// Bottleneck! Poor concurrency.
```

**Per-Elevator Lock (✅ Good)**:
```java
// Each elevator has own lock
elevator1.lock.lock();  // Thread-1 modifies Elevator-1
elevator2.lock.lock();  // Thread-2 modifies Elevator-2 simultaneously
// No waiting! High concurrency.
```

### Safety Guarantees

**1. Atomicity**:
- All state changes protected
- Never see partial updates
- Check-and-modify is atomic

**2. Visibility**:
- Lock ensures memory barriers
- All changes visible after unlock
- No stale reads

**3. Mutual Exclusion**:
- Only one thread in lock at a time
- Others wait for turn
- FIFO ordering by ReentrantLock

### Concurrent Scenarios

**Scenario 1: Two threads add stops to same elevator**
```
Thread-1: addStop(5)
Thread-2: addStop(7)

Timeline:
T1: Acquire lock
T1: Check, add 5 to upStops
T1: Release lock
T2: Acquire lock  ← Had to wait!
T2: Check, add 7 to upStops
T2: Release lock

Result: Both succeed atomically, queue has [5, 7]
```

**Scenario 2: Multiple threads modify different elevators**
```
Thread-1: Elevator-1.addStop(5)
Thread-2: Elevator-2.addStop(7)

Timeline:
T1: Acquire lock1
T2: Acquire lock2  ← No wait! Different locks!
T1: Modify Elevator-1
T2: Modify Elevator-2 (concurrent!)
T1: Release lock1
T2: Release lock2

Result: Both proceed simultaneously, high concurrency
```

**Scenario 3: Movement during request**
```
Simulator: moveOneStep()     → Modifies elevator-1
Thread: requestElevator(5)   → Calls selectBestElevator
                             → Calls elevator1.calculateScore()

Timeline:
S: lock.lock() (movement)
T: calculateScore() → lock.lock() → wait!
S: Move floor, update state
S: lock.unlock()
T: Acquire lock
T: Read currentFloor, direction (consistent state!)
T: lock.unlock()

Result: Thread sees consistent state, no corruption
```

---

## ⏱️ Time Complexity Analysis

### Operation Complexities

| Operation | Complexity | Explanation |
|-----------|-----------|-------------|
| **requestElevator()** | O(E) | Iterate through E elevators to calculate scores |
| **selectFloor()** | O(log S) | TreeSet add: log(S) where S = stops per elevator |
| **moveOneStep()** | O(log S) | TreeSet lookup and remove for arrival check |
| **calculateScore()** | O(1) | Simple arithmetic, fixed operations |
| **addStop()** | O(log S) | TreeSet add operation |
| **changeDirectionIfNeeded()** | O(1) | Check empty(), no iteration |

### Overall System Performance

```
E = number of elevators
S = average stops per elevator
R = request rate (requests/sec)

Request dispatch: O(E) per request → O(E × R) total throughput
Simulation step: O(E × log S) per step
Memory: O(E × S) for all stop queues
Thread contention: Low (per-elevator locks)
```

### Practical Performance

For typical building (10 floors, 3 elevators):
- Dispatch time: < 1ms per request
- Movement time: Negligible
- Lock contention: Minimal (3 locks, many threads)
- Throughput: Thousands of requests per second

---

## 💡 Key Design Decisions

### 1. Why TreeSet for Stops?

**Options Considered**:

**ArrayList with Manual Sort** ❌
- O(n log n) insertion (need to sort)
- Manual reverse ordering for DOWN
- Checking if element exists is O(n)

**LinkedList** ❌
- O(n) to find insertion point
- O(n) to check if exists
- No natural ordering

**PriorityQueue** ⚠️
- O(log n) insertion/removal
- O(n) to check if element exists
- Hard to implement descending order

**TreeSet** ✅
- O(log n) insertion/removal
- O(log n) contains() check
- Natural ordering built-in
- Descending order via comparator
- Provides iterator in order

### 2. Why Separate Up/Down Queues?

**Single Queue** ❌
```
Queue: [2, 5, 3, 8, 4]
Elevator at 0, direction UP
Move: 0→2→3→4→5→8→(back to 2)
Lots of back-and-forth!
```

**Dual Queues** ✅
```
upStops: [2, 3, 4, 5, 8]
downStops: []
Move: 0→2→3→4→5→8
One direction, efficient!
```

### 3. Why Score-Based Selection?

**Alternatives**:

**First Available** ❌
- Just use first idle elevator
- Doesn't consider distance
- Poor load balancing

**Closest Elevator** ⚠️
- Closest by distance
- Ignores if moving away
- Ignores current load

**Score-Based** ✅
- Balances: distance + direction + load
- Prevents overloading single elevator
- Considers direction compatibility
- Fair distribution

### 4. Why Per-Elevator Locking?

**Alternatives**:

**No Locking** ❌
- Race conditions
- Data corruption
- Concurrent requests fail

**Global Lock** ⚠️
- Thread-safe but slow
- One lock for all elevators
- Only one thread modifies at a time
- High contention

**Per-Elevator Lock** ✅
- Multiple threads can modify different elevators
- Low contention
- High concurrency
- Still thread-safe

---

## 🧪 Testing Scenarios

### Test 1: Single Request
```java
building.requestElevator(5, Direction.UP);
// Expected: One elevator moves to floor 5 and stops
```

### Test 2: Multiple Requests to Same Elevator
```java
building.selectFloor(1, 5);
building.selectFloor(1, 7);
building.selectFloor(1, 3);
// Expected: Elevator serves 5,7 (UP) then 3 (DOWN)
// Minimal direction changes
```

### Test 3: Concurrent Requests
```java
Thread t1 = new Thread(() -> building.requestElevator(4, Direction.UP));
Thread t2 = new Thread(() -> building.requestElevator(7, Direction.DOWN));
Thread t3 = new Thread(() -> building.requestElevator(2, Direction.UP));
t1.start(); t2.start(); t3.start();
t1.join(); t2.join(); t3.join();
// Expected: All requests handled concurrently without errors
```

### Test 4: Load Balancing
```java
// Give Elevator-1 many stops
building.selectFloor(1, 1);
building.selectFloor(1, 2);
building.selectFloor(1, 3);
building.selectFloor(1, 4);
building.selectFloor(1, 5);

// Request should go to less loaded elevator
building.requestElevator(2, Direction.UP);
// Expected: Elevator-2 or Elevator-3 selected (lower score)
```

### Test 5: Direction Penalty
```java
// Elevator-1 moving UP, already at floor 5
building.selectFloor(1, 8);
runSimulation(building, 3);  // Move to ~5

// Request from floor 3 going UP
// Elevator-1 already passed it, moving away
building.requestElevator(3, Direction.UP);
// Expected: Elevator-2 or Elevator-3 selected (direction penalty helps)
```

---

## 📈 Scalability Considerations

### Current Design Capabilities
- **Floors**: Supports 10-100+ floors efficiently
- **Elevators**: Works with 2-100+ elevators
- **Requests**: Thousands per second throughput
- **Concurrency**: Multiple threads safe

### Design Limitations
- **In-Memory Only**: Loses data on shutdown
- **Single JVM**: Can't scale to multiple machines
- **No Persistence**: No request logging
- **No Analytics**: Doesn't track metrics

### Future Enhancements

**1. Request Logging**
```
- Store requests in database
- Analytics: peak hours, wait times
- Audit trail
```

**2. Predictive Dispatch**
```
- ML model predicts future requests
- Pre-position elevators
- Reduce wait time
```

**3. Distributed System**
```
- Message queue for requests (Kafka/RabbitMQ)
- Multiple dispatch services
- Horizontal scaling
```

**4. Energy Optimization**
```
- Track elevator usage patterns
- Optimize routing for power
- Off-peak mode with selective elevators
```

**5. Real-Time Analytics**
```
- Dashboard showing occupancy
- Average wait times per floor
- Revenue per hour
- Peak time detection
```

---

## 🎓 Interview Discussion Points

### Questions You Might Get

**Q: Why use TreeSet instead of ArrayList?**
- TreeSet maintains sorted order automatically
- O(log n) operations instead of O(n)
- Natural ordering for both ascending (UP) and descending (DOWN)

**Q: Why two separate queues instead of one?**
- Minimizes direction changes
- Elevator serves all stops in one direction
- Single queue = inefficient back-and-forth movement

**Q: How does scoring prevent overloading?**
- Load penalty: more stops = higher score = less desirable
- Busy elevator naturally gets fewer new requests
- Distribution happens automatically

**Q: What if two elevators have same score?**
- for-loop will pick first one found
- Could add tie-breaker (elevator ID, direction preference)
- Not critical for this design

**Q: How would you handle elevator failures?**
- Health monitoring to detect failures
- Mark failed elevator unavailable
- Reassign pending requests to others

**Q: How to optimize wait times further?**
- ML-based predictive dispatch
- Dynamic scoring weights based on time of day
- Express elevators for high floors
- Real-time analytics to identify bottlenecks

---

## 🔍 Interview Talking Points

**On Dual Queues:**
"The key insight is that elevator movement is directional. Instead of one queue with mixed directions, I use two TreeSets: one for UP stops (natural ascending order) and one for DOWN stops (reverse descending order). This lets the elevator serve all stops in one direction before reversing, minimizing back-and-forth movement."

**On Scoring:**
"The scoring algorithm balances three factors: distance (how close), direction (is it on my path), and load (how busy). An elevator moving toward the request floor has score 0 for direction penalty. One moving away has penalty 20. This automatically selects the best elevator without hardcoding preferences."

**On Thread Safety:**
"Each elevator has its own ReentrantLock. When Thread-1 calls addStop() on Elevator-1, it locks only that elevator's lock. Thread-2 can simultaneously call addStop() on Elevator-2 without waiting. This fine-grained locking allows high concurrency while keeping each elevator's state consistent."

**On State Machine:**
"The elevator has four states with clear transitions. IDLE becomes MOVING_UP when stops exist above. MOVING_UP becomes DOOR_OPEN when reaching a stop. DOOR_OPEN becomes MOVING_UP/DOWN/IDLE based on remaining stops. This makes the behavior predictable and easy to reason about."

---

## 📚 Additional Resources

### Design Pattern References
- State Machine Pattern: [Refactoring Guru](https://refactoring.guru/design-patterns/state)
- Strategy Pattern: [Refactoring Guru](https://refactoring.guru/design-patterns/strategy)
- Thread Safety: [Java Concurrency in Practice](https://www.oreilly.com/library/view/java-concurrency-in/0596007566/)

### Related Problems
- Parking Lot System (Similar multi-resource scheduling)
- Load Balancer (Similar request dispatch algorithm)
- LRU Cache (Similar eviction strategy using TreeSet)

---

## 🎉 Summary

**What This Implementation Shows**:
1. Intelligent dispatch algorithm (scoring)
2. Efficient scheduling (dual queues, state machine)
3. Thread-safe concurrency (per-object locking)
4. Clean OOP design (clear responsibilities)
5. Working demo (8-step comprehensive test)

**Key Takeaways**:
- Dual queues enable efficient directional service
- Scoring algorithm balances multiple factors
- Per-elevator locking avoids bottlenecks
- State machine makes behavior clear
- TreeSet provides efficient stop management

**Interview Confidence**:
- Understand why each design decision matters
- Can explain dual queues, scoring, locking clearly
- Can walk through examples and scenarios
- Can discuss improvements and tradeoffs
- Can implement concurrent test to prove safety

---

**Ready for your interview? Run the demo, understand the code, and practice explaining the key concepts! See APPROACH.md for detailed interview strategy.**
