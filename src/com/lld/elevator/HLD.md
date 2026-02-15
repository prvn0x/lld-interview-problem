# Elevator System - High-Level Design (HLD)

## 1. Problem Statement

Design an elevator system for a multi-floor building where:
- Multiple elevators serve a building with multiple floors
- Users can request elevators from any floor (external requests)
- Users inside elevators can select destination floors (internal requests)
- The system intelligently assigns the best elevator to each request
- Elevators move efficiently using directional scheduling
- Handles concurrent requests from multiple users safely
- Provides fair, optimized service across all elevators

---

## 2. Functional Requirements

### Core Features

1. **External Requests**
   - User at any floor requests an elevator with direction (UP/DOWN)
   - System selects best available elevator
   - Dispatcher assigns request to optimal elevator

2. **Internal Requests**
   - User inside elevator selects destination floor
   - Floor is added to elevator's stop queue
   - Elevator serves the request when moving in that direction

3. **Multi-Floor Building**
   - Support building with any number of floors
   - Elevators start at ground floor (0)
   - Valid floor range: 0 to (totalFloors - 1)

4. **Directional Scheduling**
   - Separate stop queues for UP and DOWN directions
   - Elevator serves stops in current direction first
   - Changes direction only when current queue is empty
   - Efficient service: serves multiple stops per direction

5. **Elevator Movement**
   - Elevators move one floor per simulation step
   - Opens door when reaching a requested stop
   - Closes door and continues to next stop
   - Transitions between states (IDLE, MOVING, DOOR_OPEN)

6. **State Machine Behavior**
   - **IDLE**: No pending requests, not moving
   - **MOVING_UP**: Moving upward toward stops
   - **MOVING_DOWN**: Moving downward toward stops
   - **DOOR_OPEN**: Stopped at a floor with door open
   - Clear state transitions for each operation

7. **Load Balancing**
   - Dispatch algorithm considers elevator load
   - Penalizes elevators with many pending stops
   - Distributes requests across all elevators
   - Prevents overloading single elevator

8. **Thread Safety**
   - Multiple threads can request elevators simultaneously
   - No race conditions in stop queue management
   - Concurrent internal selections safe
   - All elevator states remain consistent under concurrency

---

## 3. Non-Functional Requirements

### Concurrency
- **Thread-safe elevator state**: ReentrantLock per elevator
- **Concurrent requests**: Multiple threads can request simultaneously
- **Atomic operations**: No partial state updates
- **No global bottlenecks**: Per-elevator locking, not global lock

### Performance
- **Fast dispatcher**: O(E) complexity where E = number of elevators
- **Efficient stop management**: TreeSet maintains sorted stops
- **Minimal locking**: Only lock when reading/modifying state
- **Quick assignment**: Decision in milliseconds

### Scalability
- Supports buildings with hundreds of floors
- Works with 2-100+ elevators
- Handles thousands of requests per hour
- Graceful degradation if elevators fail

### Reliability
- No double-assignment of requests
- No lost requests
- Consistent state under all conditions
- Predictable behavior under high load

---

## 4. System Architecture

```
┌──────────────────────────────────┐
│         Building                 │
│  (manages all elevators)         │
└──────────────┬───────────────────┘
               │
      ┌────────┼────────┐
      │        │        │
      ▼        ▼        ▼
   [E-1]    [E-2]    [E-3]
   Elevator Elevator Elevator
   at Floor at Floor at Floor
    0        0        0
      │        │        │
      └────────┼────────┘
               │
        ┌──────▼──────┐
        │  Dispatcher │
        │ (Controller)│
        │  Scoring    │
        │ Algorithm   │
        └──────┬──────┘
               │
    ┌──────────┼──────────┐
    │          │          │
    ▼          ▼          ▼
Request    Request    Request
Floor 5    Floor 2    Floor 8
Dir: UP    Dir: DOWN  Dir: UP
```

---

## 5. Core Components

### 5.1 Building
- **Role**: Main system coordinator
- **Manages**: All elevators in building
- **Responsibilities**:
  - Create and initialize elevators
  - Accept external requests (requestElevator)
  - Route internal selections (selectFloor)
  - Run simulation steps
  - Display system status
- **Key Methods**:
  - `requestElevator(floor, direction)` - External request
  - `selectFloor(elevatorId, floor)` - Internal selection
  - `simulateStep()` - Move all elevators one floor
  - `displayStatus()` / `displayDetailedStatus()` - Show state

### 5.2 ElevatorController (Dispatcher)
- **Role**: Intelligent request dispatcher
- **Algorithm**: Scoring-based selection
- **Scoring Factors**:
  1. **Distance**: `abs(currentFloor - requestFloor)`
  2. **Direction Penalty**:
     - 0: Moving toward request (on the way)
     - 5: IDLE (can go anywhere)
     - 20: Moving away or opposite direction
  3. **Load Penalty**: Count of pending stops
- **Selection**: Elevator with minimum score wins
- **Key Method**:
  - `selectBestElevator(elevators, requestFloor, direction)`

### 5.3 Elevator
- **Role**: Individual elevator with intelligent movement
- **State**: Position, direction, state machine, pending stops
- **Dual Queues**:
  - `upStops` (TreeSet, ascending): Floors to visit going UP
  - `downStops` (TreeSet, descending): Floors to visit going DOWN
- **Thread Safety**: ReentrantLock guards all state changes
- **Key Methods**:
  - `addStop(floor)` - Add destination to queue
  - `moveOneStep()` - Move one floor in current direction
  - `calculateScore()` - Used by dispatcher
  - Automatic door open/close at stops
  - Automatic direction reversal when queue empty

---

## 6. Key Algorithms

### 6.1 Directional Scheduling (Dual Queue Strategy)

**Goal**: Serve stops efficiently without frequent direction changes

**Data Structure**:
```
Elevator state:
- currentFloor: 3
- direction: UP
- upStops: [5, 7, 9]      (ascending order)
- downStops: [2, 1, 0]    (descending order)
```

**Process**:
```
1. Elevator at floor 3, moving UP
2. Has stops: UP=[5,7,9], DOWN=[2,1,0]
3. Serves UP direction first:
   - Move to 5 → Stop (5 in upStops)
   - Move to 7 → Stop (7 in upStops)
   - Move to 9 → Stop (9 in upStops)
   - upStops now empty
4. Reverse direction to DOWN
5. Serve DOWN stops:
   - Move to 8,7,6,5,4
   - Move to 2 → Stop (2 in downStops)
   - Move to 1 → Stop (1 in downStops)
   - Move to 0 → Stop (0 in downStops)
   - downStops now empty
6. Both queues empty → IDLE
```

**Benefits**:
- Minimizes direction changes
- Serves multiple stops per trip
- Efficient elevator usage
- Reduces waiting time

**Stop Queue Logic**:
```
If direction == UP:
  If floor > currentFloor:
    Add to upStops
  Else:
    Add to downStops

If direction == DOWN:
  If floor < currentFloor:
    Add to downStops
  Else:
    Add to upStops

If direction == IDLE:
  If floor > currentFloor:
    Add to upStops
  Else:
    Add to downStops
```

---

### 6.2 Scoring-Based Dispatcher

**Goal**: Select best elevator for each external request

**Scoring Formula**:
```
Score = Distance + DirectionPenalty + LoadPenalty

Score = |currentFloor - requestFloor|
        + directionPenalty(state, direction, requestDir)
        + (upStops.size() + downStops.size())
```

**Direction Penalty Calculation**:

```
IF elevator.state == IDLE:
    penalty = 5
    (Can go anywhere, but slight penalty for starting up)

ELSE IF elevator.direction == requestDirection:
    IF request is "on the way" (floor not yet passed):
        penalty = 0  (Perfect! Already going that way)
    ELSE:
        penalty = 20  (Already passed that floor, will need to come back)

ELSE (opposite direction or other cases):
    penalty = 20  (Must finish current direction, turn around, then serve)
```

**Example Calculation**:
```
Request: Floor 6, direction UP

Elevator-1:
  - currentFloor: 2
  - direction: UP
  - upStops: [5, 7]
  - downStops: []
  Score = |2-6| + 0 (moving toward) + 2 (pending stops)
  Score = 4 + 0 + 2 = 6

Elevator-2:
  - currentFloor: 5
  - direction: UP
  - upStops: [8, 9]
  - downStops: []
  Score = |5-6| + 0 (moving toward) + 2 (pending stops)
  Score = 1 + 0 + 2 = 3  ← Minimum! Selected

Elevator-3:
  - currentFloor: 8
  - direction: DOWN
  - upStops: []
  - downStops: [4, 2]
  Score = |8-6| + 20 (moving away) + 2 (pending stops)
  Score = 2 + 20 + 2 = 24
```

**Result**: Elevator-2 selected (score: 3)

---

### 6.3 State Machine Behavior

**States**:
- **IDLE**: No pending requests
- **MOVING_UP**: Currently moving upward
- **MOVING_DOWN**: Currently moving downward
- **DOOR_OPEN**: Stopped at floor with open door

**State Transitions**:
```
IDLE
 │
 ├─→ (addStop called)
 │   └→ changeDirectionIfNeeded()
 │       └→ MOVING_UP or MOVING_DOWN
 │
MOVING_UP
 │
 ├─→ (reach a stop in upStops)
 │   └→ openDoor()
 │       └→ DOOR_OPEN
 │
MOVING_UP
 │
 ├─→ (upStops empty, downStops has items)
 │   └→ changeDirectionIfNeeded()
 │       └→ MOVING_DOWN
 │
DOOR_OPEN
 │
 └─→ (next moveOneStep)
     └→ closeDoor()
         └→ changeDirectionIfNeeded()
             └→ IDLE / MOVING_UP / MOVING_DOWN
```

**Example Flow**:
```
1. IDLE (no stops)
2. User requests floor 5 → addStop(5)
3. State → MOVING_UP
4. Move step: 0→1 (moving)
5. Move step: 1→2 (moving)
6. Move step: 2→3 (moving)
7. Move step: 3→4 (moving)
8. Move step: 4→5 (reach stop!)
   → DOOR_OPEN
9. Move step: closeDoor()
   → IDLE (no more stops)
```

---

## 7. Thread Safety Strategy

### Fine-Grained Locking
- **Per-Elevator Lock**: Each elevator has its own ReentrantLock
- **NOT Global Lock**: Different elevators can be modified simultaneously
- **Lower Contention**: Multiple threads can modify different elevators
- **Higher Concurrency**: Threads waiting for different elevators don't block each other

### Lock Usage Pattern
```java
elevator.addStop(floor) {
    lock.lock();
    try {
        // All state modifications protected
        if (floor == currentFloor) return;

        if (direction == UP) {
            if (floor > currentFloor) {
                upStops.add(floor);
            } else {
                downStops.add(floor);
            }
        }
        // ... more logic ...
    } finally {
        lock.unlock();  // Always unlock
    }
}
```

### Concurrent Collections
- **TreeSet for Stops**: Maintains sorted order automatically
- **Not ConcurrentHashMap**: Elevator state is protected by lock

### Atomic Operations
- **Check-and-Modify**: Locked together
- **No Race Conditions**: Thread can't see partial updates
- **Consistent State**: Always protected by lock during transitions

### Safety Examples

**Scenario 1: Two threads request same elevator simultaneously**
```
Thread-1: requestElevator(5, UP)
Thread-2: requestElevator(7, UP)

Timeline:
T1: elevator.lock.lock()
T1: Check direction, add floor 5 to upStops
T1: elevator.lock.unlock()
T2: elevator.lock.lock()  ← Had to wait!
T2: Check direction, add floor 7 to upStops
T2: elevator.lock.unlock()

Result: Both requests safely added, no conflicts
```

**Scenario 2: Movement and request simultaneously**
```
Thread-1 (simulator): elevator.moveOneStep()
Thread-2 (user): elevator.addStop(6)

Timeline:
T1: elevator.lock.lock()
T1: Move currentFloor from 3→4
T1: Check if floor 4 is a stop
T1: elevator.lock.unlock()
T2: elevator.lock.lock()  ← Had to wait!
T2: Add floor 6 to upStops
T2: elevator.lock.unlock()

Result: Movement completed before new stop added, consistent state
```

---

## 8. Data Flow for External Requests

### Request Flow

```
1. User at Floor 5 presses UP button
   ↓
2. Building.requestElevator(5, UP)
   ↓
3. Controller.selectBestElevator(elevators, 5, UP)
   ↓
4. For each elevator:
   - Calculate score based on:
     * Distance from floor 5
     * Current direction vs requested UP
     * Number of pending stops
   ↓
5. Select elevator with lowest score
   ↓
6. elevator.addStop(5)
   - Lock elevator
   - Add 5 to appropriate queue (upStops or downStops)
   - Update state if IDLE
   - Unlock
   ↓
7. Elevator starts moving toward floor 5
   ↓
8. Elevator reaches floor 5
   - Opens door
   - Passenger enters
   ↓
9. simulateStep() continues moving elevator
   - Closes door
   - Moves to next stop or becomes IDLE
```

### Stop Processing Flow

```
While elevator.moveOneStep():

1. Lock elevator state
2. If state == MOVING_UP:
   - currentFloor++
   - Check: is currentFloor in upStops?
     * YES: Remove from upStops, open door, state→DOOR_OPEN
     * NO: Continue moving

3. If state == MOVING_DOWN:
   - currentFloor--
   - Check: is currentFloor in downStops?
     * YES: Remove from downStops, open door, state→DOOR_OPEN
     * NO: Continue moving

4. If state == DOOR_OPEN:
   - Close door
   - Call changeDirectionIfNeeded()
   - Check upStops: not empty? → MOVING_UP
   - Check downStops: not empty? → MOVING_DOWN
   - Both empty? → IDLE

5. Unlock elevator state
```

---

## 9. Scalability Considerations

### Current Design Strengths
- **Efficient**: O(E) complexity where E = elevators
- **Scalable Elevators**: Works with 3-100+ elevators
- **Scalable Floors**: Supports 10-100+ floor buildings
- **Scalable Requests**: Handles thousands of requests/hour
- **Concurrent**: Multiple threads request simultaneously

### Design Limitations
- **In-Memory Only**: Loses requests on shutdown
- **Single-Server**: Can't distribute across multiple machines
- **No Persistence**: No request logging
- **No Analytics**: Doesn't track wait times, usage patterns

### Future Enhancements for Scale

**1. Request Queuing**
- Add external queue for requests
- Better fairness in request assignment
- Predicted wait time for users

**2. Predictive Dispatch**
- ML model predicts future requests
- Pre-position elevators proactively
- Reduce average wait time

**3. Multi-Building**
- Distribute to multiple buildings
- Central request routing
- Load balancing across buildings

**4. Request Logging**
- Store all requests with outcomes
- Analytics: peak times, wait times
- Optimize elevator configuration

**5. Distributed System**
- Message queue (RabbitMQ/Kafka) for requests
- Multiple dispatch services
- Horizontal scaling

**6. Energy Optimization**
- Track elevator energy consumption
- Optimize routing for power efficiency
- Off-peak mode with selective elevator usage

**7. Predictive Maintenance**
- Track elevator usage patterns
- Predict maintenance needs
- Schedule maintenance during off-peak

---

## 10. Design Principles Applied

### SOLID Principles

**Single Responsibility**:
- Building: Manages elevators
- Elevator: Individual movement and state
- ElevatorController: Just dispatcher logic

**Open/Closed**:
- Can add new scoring strategies without changing existing code
- Can extend elevator behavior with new states

**Liskov Substitution**:
- Different elevators can be used interchangeably

**Interface Segregation**:
- Building only exposes needed methods
- ElevatorController focused on selection

**Dependency Inversion**:
- Building doesn't depend on specific elevator implementation

---

## 11. Comparison with Real Elevator Systems

### Similar To
- Modern building elevators (OTIS, ThyssenKrupp)
- Apartment complexes with multiple elevators
- Office buildings with bank of elevators
- Hospital elevator systems

### Our System vs Real Systems
| Aspect | Our Design | Real Systems |
|--------|-----------|--------------|
| Movement | Discrete (1 floor/step) | Continuous |
| Scheduling | Dual queue directional | SCAN/LOOK algorithm variations |
| Sensors | Simulated | Actual load/motion sensors |
| Emergency | No E-stop | Full emergency protocols |
| Accessibility | None | ADA compliance |
| Power | Unlimited | Backup power systems |
| Monitoring | Basic status | Full telemetry |

---

## 12. Key Talking Points for Interview

### What Makes This Design Good?

1. **Efficient Dispatching**
   - Scoring algorithm balances multiple factors
   - Prevents overloading single elevator
   - Minimizes average wait time

2. **Intelligent Scheduling**
   - Dual queues (UP/DOWN) minimize direction changes
   - Elevator serves multiple floors per trip
   - Reduces "back-and-forth" movements

3. **Thread Safety**
   - Per-elevator locking prevents race conditions
   - No global bottleneck
   - Safe for concurrent requests

4. **Clear State Machine**
   - Easy to reason about elevator behavior
   - Well-defined transitions
   - Easy to add new states if needed

5. **Extensibility**
   - Can add new scoring factors
   - Can change elevator strategies
   - Can add new states/behaviors

### What Would You Improve?

1. **Predictive Dispatch**
   - Use ML to predict next requests
   - Pre-position elevators
   - Reduce wait time further

2. **Real-Time Analytics**
   - Track wait times
   - Monitor throughput
   - Identify bottlenecks

3. **Dynamic Scoring**
   - Adjust weights based on time of day
   - Peak hours: more weight on load
   - Off-peak: more weight on direction

4. **Request Persistence**
   - Database logging
   - Handle elevator failures
   - Audit trail

5. **Distributed System**
   - Multiple dispatch services
   - Handle building with 20+ elevators
   - Geographic distribution

---

## 13. Summary

**System Overview**:
- Building coordinates multiple elevators
- ElevatorController uses scoring to dispatch requests
- Each Elevator manages state with dual directional queues
- Thread-safe with per-elevator locking

**Key Features**:
1. Scoring-based intelligent dispatch
2. Directional scheduling (dual queues)
3. State machine for clear behavior
4. Thread-safe concurrent requests
5. Fair load balancing

**Design Strengths**:
- Efficient: O(E) dispatcher
- Scalable: Works with many elevators
- Concurrent: Safe multi-threaded design
- Clear: Well-defined state machine
- Extensible: Easy to add features

**Next Steps**:
- See **LLD.md** for detailed class design
- See **README.md** for implementation details
- See **APPROACH.md** for interview strategy
