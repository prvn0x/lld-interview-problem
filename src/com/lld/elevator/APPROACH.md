# Elevator System - Interview Approach

## 5-Phase Interview Strategy

When you get "Design an Elevator System" in an interview, follow this structured 60-minute approach:

---

## Phase 1: Clarify Requirements (5-7 minutes)

### Questions to Ask Interviewer

1. **System Scope**
   - Single building or multiple buildings?
   - How many floors? (10, 50, 100+?)
   - How many elevators? (2, 3, 10+?)
   - Is this LLD (code-level) or HLD (architecture)?

2. **User Interactions**
   - Can users request from outside (external requests)?
   - Can users select inside elevator (internal selections)?
   - Both types or just one?

3. **Request Types**
   - External requests include direction (UP/DOWN)?
   - Internal selections are just destination floors?
   - What happens if user requests same floor?

4. **Elevator Behavior**
   - How fast does elevator move? (1 floor/second?)
   - How long door stays open?
   - Can multiple elevators serve same request?
   - What about express elevators or selective service?

5. **Concurrency Requirements**
   - Multiple users requesting simultaneously?
   - Multiple users inside elevators?
   - Thread safety required?
   - Real-time or batch processing?

6. **Optimization Goals**
   - Minimize average wait time?
   - Maximize throughput?
   - Fair service to all users?
   - Energy efficiency?

7. **Additional Features**
   - Need persistence (logging)?
   - Elevator capacity/weight limits?
   - Emergency features?
   - Accessibility features?

### Expected Answer from Interviewer

**Typical Scope**:
"Design an elevator system for a 10-floor building with 3 elevators. Support both external requests (with direction) and internal floor selections. Make it thread-safe. Optimize for fair dispatch using intelligent scoring. Focus on LLD with working code. Assume each floor takes 1 second to reach."

---

## Phase 2: High-Level Design (5-10 minutes)

### Step 1: Identify Core Entities (2 min)

Start by listing main components:
- **Building**: Manages elevators
- **Elevator**: Individual elevator with state
- **ElevatorController**: Dispatcher logic
- **Direction**: UP, DOWN, IDLE
- **ElevatorState**: IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN

### Step 2: Define Key Responsibilities (2 min)

**Building**:
- Initialize elevators
- Accept external requests
- Route internal selections
- Coordinate simulation

**Elevator**:
- Maintain current position
- Maintain pending stops
- Manage state transitions
- Calculate dispatch scores

**ElevatorController**:
- Evaluate all elevators
- Select best elevator based on score
- Consider: distance, direction, load

### Step 3: Discuss Key Challenges (3 min)

**1. Intelligent Dispatch**
- How to pick best elevator for request?
- Need scoring algorithm balancing multiple factors
- Consider: distance, direction compatibility, existing load

**2. Efficient Scheduling**
- How to minimize direction changes?
- Solution: Dual directional queues (UP and DOWN)
- Serve all stops in one direction before reversing

**3. Thread Safety**
- Multiple users requesting simultaneously?
- Solution: Per-elevator locking (fine-grained, not global)
- Prevent race conditions in stop queue updates

**4. State Management**
- Clear states: IDLE, MOVING_UP, MOVING_DOWN, DOOR_OPEN
- Transitions: Movement → Arrival → Door Open → Door Close → Next Action

**5. Stop Ordering**
- Stops should be processed in movement direction
- UP: lowest floor first
- DOWN: highest floor first
- TreeSet naturally maintains order

### Step 4: Draw System Diagram (2 min)

```
User at Floor 5 → Building.requestElevator(5, UP)
                    ↓
              ElevatorController
              Scores each elevator:
              - Elevator-1: score 10
              - Elevator-2: score 3 ← Selected!
              - Elevator-3: score 15
                    ↓
           Elevator-2.addStop(5)
                    ↓
           Elevator moves: 0→1→2→3→4→5
                    ↓
           Reaches floor 5, opens door
                    ↓
           Passenger enters
                    ↓
           Door closes, continues to next stop
```

---

## Phase 3: Low-Level Design (10-15 minutes)

### Step 1: Define Enums (2 min)

**Direction**:
```java
enum Direction {
    UP,    // Request/movement upward
    DOWN,  // Request/movement downward
    IDLE   // No direction (valid for elevator state, invalid for requests)
}
```

**ElevatorState**:
```java
enum ElevatorState {
    IDLE,        // No pending requests
    MOVING_UP,   // Moving upward
    MOVING_DOWN, // Moving downward
    DOOR_OPEN    // Stopped with door open
}
```

### Step 2: Design Elevator Class (5 min)

**Key Components**:
```java
class Elevator {
    int id;
    int currentFloor;
    Direction direction;
    ElevatorState state;

    // ⭐ Critical: Dual directional queues!
    Set<Integer> upStops;      // [3, 5, 7] (ascending)
    Set<Integer> downStops;    // [9, 8, 6] (descending)

    ReentrantLock lock;  // Thread safety!
}
```

**Why Dual Queues?**
```
Without:
  Queue: [2, 5, 3, 8, 4]
  Moving: 0→2→3→4→5→8→(back to 2) ✗ Inefficient

With dual queues:
  upStops: [2, 3, 4, 5, 8]    (ascending)
  downStops: []
  Moving: 0→2→3→4→5→8 ✓ Efficient
```

**Key Methods**:
```
+ addStop(floor)
  - Lock elevator
  - Add to upStops or downStops based on direction
  - Update state if IDLE
  - Unlock

+ moveOneStep()
  - Lock elevator
  - Increment/decrement currentFloor
  - Check if at a stop (processArrival)
  - Unlock

+ calculateScore(requestFloor, requestDirection)
  - Lock elevator (read-only)
  - Return: distance + directionPenalty + loadPenalty

- processArrival()
  - Check if currentFloor in appropriate queue
  - If yes: remove and open door

- changeDirectionIfNeeded()
  - If upStops not empty: move UP
  - Else if downStops not empty: move DOWN
  - Else: IDLE
```

### Step 3: Design Dispatcher (3 min)

**ElevatorController**:
```java
class ElevatorController {
    Elevator selectBestElevator(List<Elevator> elevators, int floor, Direction dir) {
        Elevator best = null;
        int minScore = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            int score = e.calculateScore(floor, dir);
            if (score < minScore) {
                minScore = score;
                best = e;
            }
        }
        return best;
    }
}
```

**Scoring Algorithm**:
```
Score = Distance + DirectionPenalty + LoadPenalty

Distance = abs(currentFloor - requestFloor)

DirectionPenalty:
  IF idle: 5
  ELSE IF moving toward request: 0
  ELSE: 20

LoadPenalty = number of pending stops

Example:
  Request: Floor 6, UP
  Elevator at 5, moving UP, 2 pending stops
  Score = |5-6| + 0 + 2 = 3 ← Good choice!
```

### Step 4: Design Building Class (2 min)

**Facade for entire system**:
```java
class Building {
    String buildingId;
    int totalFloors;
    List<Elevator> elevators;
    ElevatorController controller;

    void requestElevator(int floor, Direction dir) {
        // Validate floor and direction
        // Use controller to select best elevator
        // Call elevator.addStop(floor)
    }

    void selectFloor(int elevatorId, int floor) {
        // Validate elevator exists
        // Call elevator.addStop(floor)
    }

    void simulateStep() {
        // Call moveOneStep() on each elevator
    }
}
```

### Step 5: Thread Safety Strategy (3 min)

**Per-Elevator Locking**:
```java
// In Elevator class
private ReentrantLock lock = new ReentrantLock();

public void addStop(int floor) {
    lock.lock();
    try {
        // All state modifications here
        // Guaranteed atomic
    } finally {
        lock.unlock();  // Always unlock!
    }
}
```

**Benefits**:
- Multiple elevators can be modified simultaneously
- Only one thread modifies single elevator at a time
- No global bottleneck
- High concurrency

**Example**:
```
Thread-1: addStop(5) to Elevator-1
Thread-2: addStop(7) to Elevator-2

Both proceed simultaneously (different locks)
No waiting needed!
```

---

## Phase 4: Implementation (25-35 minutes)

### Step 1: Start with Simple Classes (3 min)

**Order of implementation**:
1. Direction enum (1 min)
2. ElevatorState enum (1 min)
3. Skeleton Building/Elevator/Controller classes (1 min)

### Step 2: Implement Elevator Core (8 min)

**Constructor and fields** (2 min):
```java
public class Elevator {
    private int id;
    private int currentFloor;
    private Direction direction = Direction.IDLE;
    private ElevatorState state = ElevatorState.IDLE;
    private Set<Integer> upStops = new TreeSet<>();
    private Set<Integer> downStops = new TreeSet<>((a,b) -> b.compareTo(a));
    private ReentrantLock lock = new ReentrantLock();

    public Elevator(int id, int startFloor) {
        this.id = id;
        this.currentFloor = startFloor;
    }
}
```

**addStop() method** (3 min):
```java
public void addStop(int floor) {
    lock.lock();
    try {
        if (floor == currentFloor) return;

        if (direction == Direction.UP) {
            if (floor > currentFloor) upStops.add(floor);
            else downStops.add(floor);
        } else if (direction == Direction.DOWN) {
            if (floor < currentFloor) downStops.add(floor);
            else upStops.add(floor);
        } else {  // IDLE
            if (floor > currentFloor) upStops.add(floor);
            else downStops.add(floor);
        }

        if (state == ElevatorState.IDLE) {
            changeDirectionIfNeeded();
        }
    } finally {
        lock.unlock();
    }
}
```

**calculateScore() method** (3 min):
```java
public int calculateScore(int requestFloor, Direction requestDir) {
    lock.lock();
    try {
        int distance = Math.abs(currentFloor - requestFloor);

        int penalty = 0;
        if (state == ElevatorState.IDLE) {
            penalty = 5;
        } else if (direction == requestDir) {
            if ((direction == UP && requestFloor >= currentFloor) ||
                (direction == DOWN && requestFloor <= currentFloor)) {
                penalty = 0;  // On the way
            } else {
                penalty = 20;  // Already passed
            }
        } else {
            penalty = 20;  // Opposite direction
        }

        int loadPenalty = upStops.size() + downStops.size();
        return distance + penalty + loadPenalty;
    } finally {
        lock.unlock();
    }
}
```

### Step 3: Implement Movement (5 min)

**moveOneStep() method** (3 min):
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
    } finally {
        lock.unlock();
    }
}
```

**Helper methods** (2 min):
```java
private void processArrival() {
    if (direction == Direction.UP && upStops.contains(currentFloor)) {
        upStops.remove(currentFloor);
        openDoor();
    } else if (direction == Direction.DOWN && downStops.contains(currentFloor)) {
        downStops.remove(currentFloor);
        openDoor();
    }
}

private void openDoor() {
    state = ElevatorState.DOOR_OPEN;
    System.out.println("[Elevator-" + id + "] DOOR OPEN at " + currentFloor);
}

private void closeDoor() {
    System.out.println("[Elevator-" + id + "] DOOR CLOSED at " + currentFloor);
    changeDirectionIfNeeded();
}

private void changeDirectionIfNeeded() {
    if (direction == Direction.UP || direction == Direction.IDLE) {
        if (!upStops.isEmpty()) {
            direction = Direction.UP;
            state = ElevatorState.MOVING_UP;
        } else if (!downStops.isEmpty()) {
            direction = Direction.DOWN;
            state = ElevatorState.MOVING_DOWN;
        } else {
            direction = Direction.IDLE;
            state = ElevatorState.IDLE;
        }
    } else {  // direction == DOWN
        if (!downStops.isEmpty()) {
            direction = Direction.DOWN;
            state = ElevatorState.MOVING_DOWN;
        } else if (!upStops.isEmpty()) {
            direction = Direction.UP;
            state = ElevatorState.MOVING_UP;
        } else {
            direction = Direction.IDLE;
            state = ElevatorState.IDLE;
        }
    }
}
```

### Step 4: Implement Controller (3 min)

```java
public class ElevatorController {
    public Elevator selectBestElevator(List<Elevator> elevators, int floor, Direction dir) {
        Elevator best = null;
        int minScore = Integer.MAX_VALUE;

        for (Elevator e : elevators) {
            int score = e.calculateScore(floor, dir);
            if (score < minScore) {
                minScore = score;
                best = e;
            }
        }
        return best;
    }
}
```

### Step 5: Implement Building (4 min)

```java
public class Building {
    private String buildingId;
    private int totalFloors;
    private List<Elevator> elevators;
    private ElevatorController controller;

    public Building(String id, int floors, int numElevators) {
        this.buildingId = id;
        this.totalFloors = floors;
        this.elevators = new ArrayList<>();
        this.controller = new ElevatorController();

        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i + 1, 0));
        }
    }

    public void requestElevator(int floor, Direction direction) {
        if (floor < 0 || floor >= totalFloors || direction == Direction.IDLE) {
            System.out.println("✗ Invalid request");
            return;
        }

        System.out.println("\n▶ Request: Floor " + floor + " → " + direction);
        Elevator selected = controller.selectBestElevator(elevators, floor, direction);
        if (selected != null) {
            selected.addStop(floor);
            System.out.println("  ✓ Elevator-" + selected.getId() + " assigned");
        }
    }

    public void selectFloor(int elevatorId, int floor) {
        Elevator e = getElevatorById(elevatorId);
        if (e != null) {
            e.addStop(floor);
        }
    }

    public void simulateStep() {
        for (Elevator e : elevators) {
            e.moveOneStep();
        }
    }

    public void displayStatus() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (Elevator e : elevators) {
            System.out.println("  " + e.toString());
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    // ... getter methods ...
}
```

### Step 6: Implement Main Demo (2 min)

```java
public class Main {
    public static void main(String[] args) throws InterruptedException {
        Building building = new Building("Tower-A", 10, 3);

        // Step 1: Simple request
        building.requestElevator(5, Direction.UP);
        runSimulation(building, 15);

        // Step 2: Multiple requests
        building.selectFloor(1, 7);
        building.selectFloor(1, 3);
        building.requestElevator(2, Direction.UP);
        runSimulation(building, 25);

        // Step 7: Concurrent requests
        Thread t1 = new Thread(() -> building.requestElevator(4, Direction.UP));
        Thread t2 = new Thread(() -> building.requestElevator(7, Direction.DOWN));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        runSimulation(building, 50);

        System.out.println("\n✓ Demo complete!");
    }

    private static void runSimulation(Building b, int steps) {
        for (int i = 0; i < steps; i++) {
            b.simulateStep();
            if (b.allElevatorsIdle()) {
                System.out.println("\n✓ All idle!");
                b.displayStatus();
                break;
            }
            Thread.sleep(200);
        }
    }
}
```

---

## Phase 5: Testing & Discussion (5 minutes)

### Walk Through Key Scenarios

**Scenario 1: Simple Request**
```
Request: Floor 5, UP
- Controller scores elevators
- Selects best (closest with compatible direction)
- Elevator moves: 0→1→2→3→4→5
- Door opens at 5
- Result: ✓ Success
```

**Scenario 2: Directional Scheduling**
```
Requests: [2, 5, 7] (UP) and [3, 1] (DOWN)
- Elevator has upStops=[2,5,7] and downStops=[3,1]
- Moves UP: 0→1→2→3→4→5→6→7 (serves 2,5,7)
- Reverses to DOWN: 7→6→5→4→3→2→1→0 (serves 3,1)
- Result: ✓ Efficient, minimal direction changes
```

**Scenario 3: Concurrent Requests**
```
Thread-1: requestElevator(4, UP)
Thread-2: requestElevator(7, DOWN)

Timeline:
- T1 calls selectBestElevator()
- T2 calls selectBestElevator() simultaneously
- Both get locks on different elevators
- Both complete without blocking
- Result: ✓ Concurrent, no race conditions
```

### Discuss Key Design Decisions

**1. Why TreeSet for stops?**
- "TreeSet maintains sorted order automatically. For upStops, natural order [1,3,5]. For downStops, reverse order [9,8,6]. This ensures elevators always serve stops in the right order without manual sorting."

**2. Why dual queues?**
- "Without dual queues, elevator would serve [2,5,3,7] like 2→3→5→7→(back to 3). With dual queues: UP=[2,3,5,7], DOWN=[]. Elevator serves all in one direction. Much more efficient!"

**3. Why per-elevator locking?**
- "If I used global lock, only one thread could modify any elevator. That's slow. With per-elevator lock, Thread-1 can modify Elevator-1 while Thread-2 modifies Elevator-2. Parallel modification of different elevators. Higher concurrency, better performance."

**4. Why scoring algorithm?**
- "Distance alone isn't good (closest might be overloaded). Direction matters (if already going opposite way, adds delay). Load matters (busy elevator should get fewer requests). Scoring balances all three factors to pick the best elevator."

### Handle Interviewer Questions

**Q: What if two elevators have same score?**
- "The for-loop will pick the first one found. We could break ties with elevator ID. Not critical for this design."

**Q: What about elevator capacity?**
- "Good point. We'd add a capacity check in addStop(). If at capacity, would get different score penalty or reject. For this design, assuming unlimited capacity."

**Q: How do you handle elevator failures?**
- "In production, we'd have health monitoring. Mark failed elevator as unavailable. Reassign pending requests to others. For this design, all elevators always available."

**Q: How would you optimize wait time further?**
- "Could predict future requests using ML. Pre-position elevators. Could use dynamic scoring weights based on time of day. Could implement express elevators for high floors."

---

## Time Management (60-minute interview)

| Phase | Time | Activities |
|-------|------|------------|
| **Clarify** | 5-7 min | Ask 7 questions, understand scope |
| **HLD** | 5-10 min | Identify entities, discuss challenges, draw diagram |
| **LLD** | 10-15 min | Design classes, explain dual queues, discuss locking |
| **Code** | 25-35 min | Implement Elevator, Controller, Building, demo |
| **Test** | 5 min | Walk through scenarios, discuss improvements |

**Time-Saving Tips**:
- Use skeleton code first, add details only if time permits
- Don't optimize prematurely (readable > perfect)
- Focus on core logic first (dual queues, scoring, movement)
- Mention but don't implement: error handling, edge cases (if time running short)

---

## Key Talking Points During Interview

### When Explaining Dual Queues
"Instead of one queue with mixed directions, I'm using two TreeSets: upStops and downStops. This lets the elevator serve all stops in one direction before reversing. It's like an elevator scheduler rather than just following orders. Much more efficient than the naive approach."

### When Implementing addStop()
"The key insight is: which queue to add the stop to? It depends on current direction. If moving UP and floor is above me, add to upStops. If it's below, add to downStops (I'll get to it after reversing). This logic automatically groups stops by direction."

### When Explaining calculateScore()
"The score has three components: distance (how far away), direction (is this on my current path), and load (how busy am I). A busy elevator that's already going the right way might have score 3. An idle elevator far away might have score 20. Lower score wins. This balances multiple objectives smartly."

### When Discussing Thread Safety
"Each elevator has its own ReentrantLock. When a thread calls addStop(), it locks just that elevator's lock. Other threads can simultaneously modify other elevators. This is fine-grained locking. If I used one global lock, only one thread could modify anything, which is slow. Per-elevator locking allows high concurrency."

### When Asked About Improvements
"For production, I'd add: (1) Request persistence to log all requests, (2) Real-time analytics to track wait times and optimize scoring weights, (3) Predictive dispatch using ML to pre-position elevators, (4) Health monitoring to detect failed elevators, (5) Distributed system with multiple dispatch services for larger buildings."

---

## Common Mistakes to Avoid

❌ **DON'T**:
1. Forget thread safety (critical!)
2. Use single global lock for all elevators
3. Ignore state machine transitions
4. Implement naive queue (instead of dual queues)
5. Hardcode scoring algorithm (make it flexible)
6. Add unnecessary complexity early
7. Skip the working demo

✅ **DO**:
1. Start with clear requirements
2. Draw system diagram
3. Explain dual queue benefits
4. Implement per-elevator locking
5. Use TreeSet for automatic sorting
6. Keep code readable
7. Demonstrate with concurrent requests
8. Discuss improvements and tradeoffs

---

## What Interviewers Look For

### Strong Signals ✅
- Immediately identifies concurrent requests as key challenge
- Proposes per-elevator locking (not global lock)
- Explains dual queue scheduling clearly
- Scoring algorithm balances multiple factors
- Implements thread-safe addStop() correctly
- Demonstrates concurrent requests test
- Discusses tradeoffs and improvements

### Weak Signals ❌
- Ignores thread safety
- Uses synchronized(this) or global lock
- Can't explain why dual queues matter
- Hardcodes elevator selection logic
- No concurrent testing
- Can't handle edge cases (same floor, idle direction)
- No discussion of improvements

---

## Summary: Your Interview Approach

1. **Clarify** → Ask about floors, elevators, concurrency, optimization goals
2. **HLD** → Identify entities: Building, Elevator, Controller
3. **Design** → Explain dual queues, scoring, per-elevator locking
4. **Code** → Implement Elevator with TreeSets, Controller with scoring
5. **Test** → Show concurrent requests working correctly
6. **Discuss** → Talk about improvements, tradeoffs, scalability

**The Three Key Ideas to Master**:
1. **Dual Queues**: Why they enable efficient directional scheduling
2. **Scoring Algorithm**: How to select best elevator (distance + direction + load)
3. **Per-Elevator Locking**: How to allow concurrent modification safely

**Remember**: Interviewers want to see you think systematically, handle concurrency correctly, and write clean code. Don't try to be perfect; aim for clear, working code with good design decisions.

---

**Now go build this in code! See README.md for detailed implementation notes.**
