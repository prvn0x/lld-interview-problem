# Elevator System - Low-Level Design (LLD)

## 1. Class Diagram Overview

```
┌─────────────────────────────────────────────────────────┐
│                 Building                                │
│  - buildingId: String                                   │
│  - totalFloors: int                                     │
│  - elevators: List<Elevator>                            │
│  - controller: ElevatorController                       │
├─────────────────────────────────────────────────────────┤
│  + requestElevator(floor, direction)                    │
│  + selectFloor(elevatorId, floor)                       │
│  + simulateStep()                                       │
│  + displayStatus()                                      │
│  + allElevatorsIdle(): boolean                          │
└─────────────────────────────────────────────────────────┘
              │
              │ uses
              ▼
┌──────────────────────────────────────┐
│   ElevatorController (Dispatcher)    │
├──────────────────────────────────────┤
│  + selectBestElevator(elevators,     │
│      requestFloor, direction)        │
│      : Elevator                      │
└──────────────────────────────────────┘
              │ uses
              ▼
┌─────────────────────────────────────────────────────────┐
│              Elevator (Per Building)                    │
│  - id: int                                              │
│  - currentFloor: int                                    │
│  - direction: Direction                                │
│  - state: ElevatorState                                │
│  - upStops: Set<Integer>   (ascending TreeSet)         │
│  - downStops: Set<Integer> (descending TreeSet)        │
│  - lock: ReentrantLock    (Thread Safety!)             │
├─────────────────────────────────────────────────────────┤
│  + addStop(floor)                                       │
│  + moveOneStep()                                        │
│  + calculateScore(requestFloor, direction): int        │
│  + getCurrentFloor(): int                              │
│  + getDirection(): Direction                           │
│  + getState(): ElevatorState                           │
│  + getPendingStops(): int                              │
│  - processArrival()      [Private]                     │
│  - openDoor()            [Private]                     │
│  - closeDoor()           [Private]                     │
│  - changeDirectionIfNeeded()  [Private]                │
└─────────────────────────────────────────────────────────┘

┌─────────────────┐
│   Direction     │
├─────────────────┤
│ UP              │
│ DOWN            │
│ IDLE            │
└─────────────────┘

┌─────────────────┐
│  ElevatorState  │
├─────────────────┤
│ IDLE            │
│ MOVING_UP       │
│ MOVING_DOWN     │
│ DOOR_OPEN       │
└─────────────────┘
```

---

## 2. Detailed Class Design

### 2.1 Building Class

**Package**: `com.lld.elevator.models`

**Responsibility**: Main system coordinator managing all elevators

```java
public class Building {
    private final String buildingId;
    private final int totalFloors;
    private final List<Elevator> elevators;
    private final ElevatorController controller;

    public Building(String buildingId, int totalFloors, int numElevators) {
        // Initialize with all elevators at ground floor (0)
        // Create ElevatorController for dispatching
    }

    public void requestElevator(int floor, Direction direction) {
        // External request from a floor
        // Validate: 0 <= floor < totalFloors
        // Validate: direction != IDLE
        // Use dispatcher to select best elevator
        // Call elevator.addStop(floor)
    }

    public void selectFloor(int elevatorId, int destination) {
        // Internal selection from inside elevator
        // Validate elevator ID exists
        // Validate: 0 <= destination < totalFloors
        // Call elevator.addStop(destination)
    }

    public void simulateStep() {
        // Run one simulation step
        // Call moveOneStep() on all elevators
        // This moves all elevators one floor
    }

    public void displayStatus() {
        // Print current status of all elevators
        // Format: Elevator-ID [Floor, State, Direction, Pending]
    }

    public void displayDetailedStatus() {
        // Print detailed status including pending stops
        // Format: Elevator-ID [Floor, State, Direction, UpStops, DownStops]
    }

    public boolean allElevatorsIdle() {
        // Check if all elevators have no pending stops
        // Return true only if all elevators are completely idle
    }

    public List<Elevator> getElevators() { }
    public int getTotalFloors() { }
}
```

**Key Points**:
- Building is the facade for the entire system
- Delegates actual elevator logic to individual Elevator objects
- Uses ElevatorController for intelligent dispatch
- No locking at Building level (per-elevator locking in Elevator)

---

### 2.2 ElevatorController Class (Dispatcher)

**Package**: `com.lld.elevator.services`

**Responsibility**: Assign best elevator for each external request

**Algorithm**: Scoring-based selection

```java
public class ElevatorController {

    public Elevator selectBestElevator(
        List<Elevator> elevators,
        int requestFloor,
        Direction direction) {

        // For each elevator, calculate score
        // Score = distance + directionPenalty + loadPenalty
        // Return elevator with minimum score

        // Score Components:
        // 1. Distance: abs(elevatorCurrentFloor - requestFloor)
        // 2. Direction Penalty:
        //    - 0 if moving toward request
        //    - 5 if idle
        //    - 20 if moving away
        // 3. Load Penalty: number of pending stops
    }
}
```

**Scoring Examples**:

```
Request: Floor 6, Direction UP

Elevator-1 at floor 2, moving UP with 2 stops:
  Distance = |2 - 6| = 4
  Direction penalty = 0 (moving UP, floor 6 is ahead)
  Load penalty = 2
  Total Score = 4 + 0 + 2 = 6

Elevator-2 at floor 5, moving UP with 2 stops:
  Distance = |5 - 6| = 1
  Direction penalty = 0 (moving UP, floor 6 is ahead)
  Load penalty = 2
  Total Score = 1 + 0 + 2 = 3 ← SELECTED (lowest)

Elevator-3 at floor 8, moving DOWN with 2 stops:
  Distance = |8 - 6| = 2
  Direction penalty = 20 (moving DOWN, opposite to UP)
  Load penalty = 2
  Total Score = 2 + 20 + 2 = 24
```

**Key Points**:
- Stateless service (no instance variables)
- Only focuses on selection logic
- Doesn't modify elevator state
- Can be called from multiple threads safely

---

### 2.3 Elevator Class (Core!)

**Package**: `com.lld.elevator.models`

**Responsibility**: Individual elevator with intelligent movement and state management

**Thread Safety**: ReentrantLock guards all state modifications

```java
public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;

    // Dual queues: UP stops (ascending), DOWN stops (descending)
    private final Set<Integer> upStops;      // TreeSet in natural order
    private final Set<Integer> downStops;    // TreeSet in reverse order

    private final ReentrantLock lock;  // ⭐ CRITICAL: Thread safety

    public Elevator(int id, int startFloor) {
        this.id = id;
        this.currentFloor = startFloor;
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.upStops = new TreeSet<>();                    // [1, 3, 5]
        this.downStops = new TreeSet<>((a,b) -> b.compareTo(a));  // [9, 7, 5]
        this.lock = new ReentrantLock();
    }
}
```

#### addStop(floor) Method

```java
public void addStop(int floor) {
    lock.lock();
    try {
        if (floor == currentFloor) return;  // Already here

        // Determine which queue to add to based on direction
        if (direction == Direction.UP) {
            if (floor > currentFloor) {
                upStops.add(floor);        // On the way up
            } else {
                downStops.add(floor);      // Below, after reversing
            }
        } else if (direction == Direction.DOWN) {
            if (floor < currentFloor) {
                downStops.add(floor);      // On the way down
            } else {
                upStops.add(floor);        // Above, after reversing
            }
        } else {  // IDLE
            if (floor > currentFloor) {
                upStops.add(floor);
            } else {
                downStops.add(floor);
            }
        }

        // If idle, start moving
        if (state == ElevatorState.IDLE) {
            changeDirectionIfNeeded();
        }
    } finally {
        lock.unlock();
    }
}
```

**Logic Breakdown**:
1. Lock to prevent concurrent modifications
2. Skip if already at floor
3. Add to appropriate queue based on current direction
4. If idle, trigger state change
5. Always unlock in finally

#### moveOneStep() Method

```java
public void moveOneStep() {
    lock.lock();
    try {
        if (state == ElevatorState.MOVING_UP) {
            currentFloor++;
            System.out.println("[Elevator-" + id + "] Moving UP to " + currentFloor);
            processArrival();
        } else if (state == ElevatorState.MOVING_DOWN) {
            currentFloor--;
            System.out.println("[Elevator-" + id + "] Moving DOWN to " + currentFloor);
            processArrival();
        } else if (state == ElevatorState.DOOR_OPEN) {
            closeDoor();
        }
        // IDLE state: no movement
    } finally {
        lock.unlock();
    }
}
```

**Logic Breakdown**:
1. Lock elevator state
2. If MOVING_UP: increment floor, process arrival
3. If MOVING_DOWN: decrement floor, process arrival
4. If DOOR_OPEN: close door and determine next action
5. If IDLE: do nothing
6. Unlock

#### processArrival() Method (Private)

```java
private void processArrival() {
    boolean isStop = false;

    // Check if current floor is a scheduled stop
    if (direction == Direction.UP && upStops.contains(currentFloor)) {
        upStops.remove(currentFloor);
        isStop = true;
    } else if (direction == Direction.DOWN && downStops.contains(currentFloor)) {
        downStops.remove(currentFloor);
        isStop = true;
    }

    if (isStop) {
        openDoor();  // Stop and open door
    }
}
```

**Logic Breakdown**:
1. Check if current floor is in the appropriate queue
2. If yes: remove from queue and open door
3. If no: continue moving

#### openDoor() Method (Private)

```java
private void openDoor() {
    state = ElevatorState.DOOR_OPEN;
    System.out.println("[Elevator-" + id + "] DOOR OPEN at floor " + currentFloor);
}
```

#### closeDoor() Method (Private)

```java
private void closeDoor() {
    System.out.println("[Elevator-" + id + "] DOOR CLOSED at floor " + currentFloor);
    changeDirectionIfNeeded();  // Determine next action
}
```

#### changeDirectionIfNeeded() Method (Private - Key Logic!)

```java
private void changeDirectionIfNeeded() {
    if (direction == Direction.UP || direction == Direction.IDLE) {
        // Currently going UP or IDLE
        if (!upStops.isEmpty()) {
            direction = Direction.UP;
            state = ElevatorState.MOVING_UP;
        } else if (!downStops.isEmpty()) {
            // Need to reverse
            direction = Direction.DOWN;
            state = ElevatorState.MOVING_DOWN;
            System.out.println("[Elevator-" + id + "] Changing direction to DOWN");
        } else {
            // Both queues empty
            direction = Direction.IDLE;
            state = ElevatorState.IDLE;
            System.out.println("[Elevator-" + id + "] Became IDLE");
        }
    } else if (direction == Direction.DOWN) {
        // Currently going DOWN
        if (!downStops.isEmpty()) {
            direction = Direction.DOWN;
            state = ElevatorState.MOVING_DOWN;
        } else if (!upStops.isEmpty()) {
            // Need to reverse
            direction = Direction.UP;
            state = ElevatorState.MOVING_UP;
            System.out.println("[Elevator-" + id + "] Changing direction to UP");
        } else {
            // Both queues empty
            direction = Direction.IDLE;
            state = ElevatorState.IDLE;
            System.out.println("[Elevator-" + id + "] Became IDLE");
        }
    }
}
```

**Decision Logic**:
```
IF direction UP (or IDLE):
  IF upStops not empty → continue UP (state: MOVING_UP)
  ELSE IF downStops not empty → reverse to DOWN (state: MOVING_DOWN)
  ELSE → become IDLE (state: IDLE)

IF direction DOWN:
  IF downStops not empty → continue DOWN (state: MOVING_DOWN)
  ELSE IF upStops not empty → reverse to UP (state: MOVING_UP)
  ELSE → become IDLE (state: IDLE)
```

#### calculateScore() Method

```java
public int calculateScore(int requestFloor, Direction requestDirection) {
    lock.lock();
    try {
        // Distance from request floor
        int distance = Math.abs(currentFloor - requestFloor);

        // Direction penalty calculation
        int directionPenalty = 0;

        if (state == ElevatorState.IDLE) {
            directionPenalty = 5;  // Can go anywhere, slight penalty
        } else if (direction == requestDirection) {
            // Moving in same direction as request
            if ((direction == Direction.UP && requestFloor >= currentFloor) ||
                (direction == Direction.DOWN && requestFloor <= currentFloor)) {
                directionPenalty = 0;  // On the way!
            } else {
                directionPenalty = 20;  // Already passed
            }
        } else {
            directionPenalty = 20;  // Opposite direction
        }

        // Load penalty: more stops = less desirable
        int loadPenalty = upStops.size() + downStops.size();

        return distance + directionPenalty + loadPenalty;
    } finally {
        lock.unlock();
    }
}
```

#### Getter Methods (All Thread-Safe)

```java
public int getId() { return id; }  // Immutable, no lock needed

public int getCurrentFloor() {
    lock.lock();
    try { return currentFloor; }
    finally { lock.unlock(); }
}

public Direction getDirection() {
    lock.lock();
    try { return direction; }
    finally { lock.unlock(); }
}

public ElevatorState getState() {
    lock.lock();
    try { return state; }
    finally { lock.unlock(); }
}

public int getPendingStops() {
    lock.lock();
    try { return upStops.size() + downStops.size(); }
    finally { lock.unlock(); }
}
```

---

### 2.4 Direction Enum

**Package**: `com.lld.elevator.enums`

**Responsibility**: Enumeration of possible direction values

```java
public enum Direction {
    UP,      // Moving toward higher floor numbers
    DOWN,    // Moving toward lower floor numbers
    IDLE     // Not moving (for requests, invalid; for elevator state, valid)
}
```

**Usage**:
- External requests: Direction.UP or Direction.DOWN
- Elevator state: Can be UP, DOWN, or IDLE
- Invalid: External request with IDLE direction

---

### 2.5 ElevatorState Enum

**Package**: `com.lld.elevator.enums`

**Responsibility**: State machine states for elevator behavior

```java
public enum ElevatorState {
    IDLE,          // Not moving, no pending requests
    MOVING_UP,     // Currently moving upward
    MOVING_DOWN,   // Currently moving downward
    DOOR_OPEN      // Stopped with door open
}
```

**State Transitions**:
```
IDLE
  ↓ (addStop called)
MOVING_UP / MOVING_DOWN
  ↓ (reach stop)
DOOR_OPEN
  ↓ (next step)
MOVING_UP / MOVING_DOWN or IDLE (depending on pending)
```

---

## 3. Dual Queue Strategy (Directional Scheduling)

### Why Dual Queues?

**Problem**: Elevator changing direction frequently
```
Without dual queues:
Queue: [2, 5, 3, 7, 4]

Elevator at floor 0:
→ 2 (up)
→ 3 (up)
→ 4 (up)
→ 5 (up)
→ 7 (up)
→ Back down to 2 ✗ Inefficient!
```

**Solution: Dual Queues**
```
upStops: [2, 3, 4, 5, 7]    (ascending)
downStops: []

Elevator at floor 0:
→ 2, 3, 4, 5, 7 (all in one direction)
→ Both queues empty, IDLE

Much more efficient!
```

### TreeSet Implementation

**upStops**: Natural order (ascending)
```java
Set<Integer> upStops = new TreeSet<>();
upStops.add(5);
upStops.add(3);
upStops.add(7);

// Automatically sorted: [3, 5, 7]
// First element: 3 (next floor)
```

**downStops**: Reverse order (descending)
```java
Set<Integer> downStops = new TreeSet<>((a, b) -> b.compareTo(a));
downStops.add(5);
downStops.add(3);
downStops.add(7);

// Reverse sorted: [7, 5, 3]
// First element: 7 (next floor when going down from 8)
```

### Logic Example

```
Scenario: Elevator at floor 5, has stops [2, 3, 5, 7, 8]

Step 1: addStop(2) - floor < currentFloor → downStops.add(2)
Step 2: addStop(3) - floor < currentFloor → downStops.add(3)
Step 3: addStop(5) - floor == currentFloor → skip
Step 4: addStop(7) - floor > currentFloor → upStops.add(7)
Step 5: addStop(8) - floor > currentFloor → upStops.add(8)

Result:
  upStops: [7, 8]
  downStops: [3, 2]  (reversed)
  direction: UP

Elevator moves:
  5 → 6 → 7 (STOP, door open, remove 7)
  7 → 8 (STOP, door open, remove 8)
  8 → 7 → 6 → 5 → 4 → 3 (STOP, door open, remove 3)
  3 → 2 (STOP, door open, remove 2)
  Both empty → IDLE
```

---

## 4. Thread Safety Analysis

### Thread Model

**Multiple Threads**:
- 1+ Main threads calling requestElevator()
- 1+ Threads calling selectFloor()
- 1 Simulator thread calling moveOneStep() repeatedly
- Each thread operates on potentially different elevators

### Lock Strategy

**Per-Elevator Locking**:
```java
// Elevator-1 is being modified by Thread-A
lock1.lock();
  // Thread-B trying to modify Elevator-2 doesn't wait
  lock2.lock();
```

**Benefits**:
- Multiple threads can modify different elevators simultaneously
- No global bottleneck
- High concurrency

### Race Condition Prevention

**Scenario 1: Two threads call addStop() simultaneously**
```
Thread-1: addStop(5)  ← Lock acquired
Thread-2: addStop(7)  ← Waits for Thread-1's lock

Timeline:
1. T1 locks
2. T1 checks direction, adds 5 to queue
3. T1 unlocks
4. T2 acquires lock
5. T2 checks direction, adds 7 to queue
6. T2 unlocks

Result: Both adds succeed atomically, no conflicts
```

**Scenario 2: addStop() and moveOneStep() simultaneously**
```
Thread-1: moveOneStep()    ← Lock acquired
Thread-2: addStop(floor)   ← Waits

Timeline:
1. T1 locks
2. T1 moves floor, checks arrival, updates state
3. T1 unlocks
4. T2 acquires lock
5. T2 adds stop to queue
6. T2 unlocks

Result: Movement completes before stop added, consistent state
```

**Scenario 3: Multiple reads (getters) concurrent with writes**
```
Thread-1: calculateScore()     ← Lock acquired
Thread-2: getCurrentFloor()    ← Waits

Timeline:
1. T1 locks
2. T1 reads currentFloor, direction, calculates score
3. T1 unlocks
4. T2 locks
5. T2 reads currentFloor
6. T2 unlocks

Result: T2 sees consistent state from either before or after T1
```

### Lock Usage Guarantees

**1. Atomicity**:
- All state changes protected by lock
- Never see partial updates
- State always consistent

**2. Visibility**:
- Lock ensures memory visibility
- All changes visible to other threads after unlock
- No stale reads

**3. Ordering**:
- Multiple operations ordered by lock acquisition
- FIFO fairness (ReentrantLock behavior)
- No thread starvation

---

## 5. SOLID Principles Applied

### Single Responsibility Principle
- **Building**: Coordinate elevators
- **Elevator**: Manage individual movement
- **ElevatorController**: Select best elevator
- Each class has one reason to change

### Open/Closed Principle
- Can add new elevator scoring strategies without changing Elevator
- Can add new states without changing core logic
- Can extend behavior via inheritance if needed

### Liskov Substitution Principle
- Any Elevator can be used in place of another
- Interface contracts honored consistently

### Interface Segregation Principle
- Building exposes only needed public methods
- No fat interfaces with unused methods

### Dependency Inversion Principle
- Building depends on Elevator abstraction, not implementation details
- ElevatorController depends on Elevator interface

---

## 6. Design Patterns Used

### 1. **State Machine Pattern**
- Elevator has discrete states: IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
- Transitions between states based on conditions
- Each state has specific behavior (move, open door, etc.)

```java
Enum: ElevatorState
State transitions via changeDirectionIfNeeded()
Clear state-to-state flow
```

### 2. **Strategy Pattern** (Scoring)
- Different scoring strategies possible
- Could implement AlternativeDispatcher with different algorithm
- Easy to extend without modifying existing code

### 3. **Thread Safety Pattern**
- Per-object locking (ReentrantLock)
- Lock acquisition in try-finally
- Clear critical section

### 4. **Facade Pattern**
- Building provides simple interface
- Hides complexity of Elevator + Controller

### 5. **Enum with Behavior Pattern**
- Direction and ElevatorState enums
- Can add methods to enums if needed

---

## 7. Time Complexity Analysis

### Operations

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| requestElevator() | O(E) | Iterate through E elevators to calculate scores |
| selectFloor() | O(log S) | TreeSet add operation (S = stops) |
| moveOneStep() | O(log S) | TreeSet lookup + remove for arrival check |
| calculateScore() | O(1) | Simple arithmetic, fixed number of reads |
| addStop() | O(log S) | TreeSet add operation |
| changeDirectionIfNeeded() | O(1) | Check empty(), no iteration |

### Overall System

| Metric | Complexity |
|--------|-----------|
| Request dispatch | O(E) where E = elevators |
| Simulation step | O(E × log S) where E = elevators, S = stops per elevator |
| Memory per elevator | O(S) where S = total stops |
| Thread contention | Low (per-elevator locks) |

---

## 8. Data Structure Choices

### Why TreeSet for Stops?

**Alternatives Considered**:

1. **ArrayList with manual sorting**
   - ✗ O(n log n) insertion with sorting
   - ✗ Manual reverse ordering for downStops
   - ✓ Simple structure

2. **PriorityQueue**
   - ✗ O(n) to check if element exists
   - ✓ Fast insertion/removal
   - ✓ Natural ordering

3. **TreeSet (Chosen)**
   - ✓ O(log n) insertion
   - ✓ O(log n) removal
   - ✓ O(1) contains check (for arrival detection)
   - ✓ Natural ordering (ascending/descending)
   - ✓ Sorted iteration

**Decision**: TreeSet provides best balance of operations

---

## 9. Edge Cases Handled

### 1. Same Floor Request
```java
if (floor == currentFloor) return;
```

### 2. Invalid Floor
```java
if (floor < 0 || floor >= totalFloors) {
    System.out.println("Invalid floor");
    return;
}
```

### 3. IDLE Direction Request
```java
if (direction == Direction.IDLE) {
    System.out.println("Cannot request with IDLE");
    return;
}
```

### 4. Empty Elevator ID
```java
Elevator elevator = getElevatorById(elevatorId);
if (elevator == null) {
    System.out.println("Elevator not found");
    return;
}
```

### 5. All Elevators Idle
```java
public boolean allElevatorsIdle() {
    for (Elevator e : elevators) {
        if (e.getPendingStops() > 0) return false;
    }
    return true;
}
```

---

## 10. Summary

**Key Classes**:
1. **Building**: System coordinator
2. **Elevator**: Individual elevator with dual queues
3. **ElevatorController**: Intelligent dispatcher
4. **Direction**: Enum for direction values
5. **ElevatorState**: Enum for state machine

**Key Features**:
- Dual queue directional scheduling (upStops, downStops)
- Scoring-based dispatcher (distance + direction + load)
- State machine (IDLE → MOVING → DOOR_OPEN → ...)
- Per-elevator thread-safe locking
- TreeSet for efficient stop management

**Design Strengths**:
- Thread-safe without global locking
- Efficient dispatcher: O(E) complexity
- Clear state transitions
- Handles all edge cases

**Next**: See **APPROACH.md** for interview strategy and **README.md** for implementation details.
