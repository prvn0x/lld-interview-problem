package com.lld.elevator.models;

import com.lld.elevator.enums.Direction;
import com.lld.elevator.enums.ElevatorState;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Elevator with directional scheduling and state machine
 *
 * Thread-safe implementation using:
 * - ReentrantLock for state modifications
 * - TreeSet for ordered stop queues
 *
 * Scheduling:
 * - Maintains separate queues for UP and DOWN stops
 * - Serves stops in direction of movement
 * - Changes direction only when current direction queue is empty
 */
public class Elevator {
    private final int id;
    private int currentFloor;
    private Direction direction;
    private ElevatorState state;

    // Thread-safe stop queues
    // TreeSet maintains natural ordering: ascending for upStops, descending for downStops
    private final Set<Integer> upStops;      // Ascending order
    private final Set<Integer> downStops;    // Descending order (using reverseOrder)

    private final ReentrantLock lock;

    public Elevator(int id, int startFloor) {
        this.id = id;
        this.currentFloor = startFloor;
        this.direction = Direction.IDLE;
        this.state = ElevatorState.IDLE;
        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>((a, b) -> b.compareTo(a));  // Descending
        this.lock = new ReentrantLock();
    }

    /**
     * Add a stop to appropriate queue based on current direction and position
     *
     * Logic:
     * - If moving UP: add to upStops if floor >= current, else to downStops
     * - If moving DOWN: add to downStops if floor <= current, else to upStops
     * - If IDLE: add to appropriate queue based on floor position
     */
    public void addStop(int floor) {
        lock.lock();
        try {
            if (floor == currentFloor) {
                // Already at this floor, no need to add
                return;
            }

            if (direction == Direction.UP) {
                if (floor > currentFloor) {
                    upStops.add(floor);
                } else {
                    downStops.add(floor);
                }
            } else if (direction == Direction.DOWN) {
                if (floor < currentFloor) {
                    downStops.add(floor);
                } else {
                    upStops.add(floor);
                }
            } else {
                // IDLE - add to appropriate queue
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

    /**
     * Move elevator one floor in current direction
     * Called during simulation step
     */
    public void moveOneStep() {
        lock.lock();
        try {
            if (state == ElevatorState.MOVING_UP) {
                currentFloor++;
                System.out.println(String.format("  [Elevator-%d] Moving UP to floor %d", id, currentFloor));
                processArrival();
            } else if (state == ElevatorState.MOVING_DOWN) {
                currentFloor--;
                System.out.println(String.format("  [Elevator-%d] Moving DOWN to floor %d", id, currentFloor));
                processArrival();
            } else if (state == ElevatorState.DOOR_OPEN) {
                // Door is open, close it and continue
                closeDoor();
            }
            // IDLE - no movement
        } finally {
            lock.unlock();
        }
    }

    /**
     * Process arrival at current floor
     * Check if this floor is a stop, if yes open door
     */
    private void processArrival() {
        boolean isStop = false;

        if (direction == Direction.UP && upStops.contains(currentFloor)) {
            upStops.remove(currentFloor);
            isStop = true;
        } else if (direction == Direction.DOWN && downStops.contains(currentFloor)) {
            downStops.remove(currentFloor);
            isStop = true;
        }

        if (isStop) {
            openDoor();
        }
    }

    /**
     * Open door at current floor
     */
    private void openDoor() {
        state = ElevatorState.DOOR_OPEN;
        System.out.println(String.format("  [Elevator-%d] DOOR OPEN at floor %d", id, currentFloor));
    }

    /**
     * Close door and decide next action
     */
    private void closeDoor() {
        System.out.println(String.format("  [Elevator-%d] DOOR CLOSED at floor %d", id, currentFloor));
        changeDirectionIfNeeded();
    }

    /**
     * Change direction based on queue status
     *
     * Logic:
     * - Continue in same direction if stops exist
     * - Reverse if opposite direction has stops
     * - Become IDLE if both queues empty
     */
    private void changeDirectionIfNeeded() {
        if (direction == Direction.UP || direction == Direction.IDLE) {
            if (!upStops.isEmpty()) {
                direction = Direction.UP;
                state = ElevatorState.MOVING_UP;
            } else if (!downStops.isEmpty()) {
                direction = Direction.DOWN;
                state = ElevatorState.MOVING_DOWN;
                System.out.println(String.format("  [Elevator-%d] Changing direction to DOWN", id));
            } else {
                direction = Direction.IDLE;
                state = ElevatorState.IDLE;
                System.out.println(String.format("  [Elevator-%d] Became IDLE", id));
            }
        } else if (direction == Direction.DOWN) {
            if (!downStops.isEmpty()) {
                direction = Direction.DOWN;
                state = ElevatorState.MOVING_DOWN;
            } else if (!upStops.isEmpty()) {
                direction = Direction.UP;
                state = ElevatorState.MOVING_UP;
                System.out.println(String.format("  [Elevator-%d] Changing direction to UP", id));
            } else {
                direction = Direction.IDLE;
                state = ElevatorState.IDLE;
                System.out.println(String.format("  [Elevator-%d] Became IDLE", id));
            }
        }
    }

    /**
     * Calculate score for dispatcher
     * Lower score = better fit
     *
     * Score = distance + directionPenalty + loadPenalty
     */
    public int calculateScore(int requestFloor, Direction requestDirection) {
        lock.lock();
        try {
            int distance = Math.abs(currentFloor - requestFloor);

            // Direction penalty
            int directionPenalty = 0;
            if (state == ElevatorState.IDLE) {
                directionPenalty = 5;
            } else if (direction == requestDirection) {
                // Moving in same direction
                if ((direction == Direction.UP && requestFloor >= currentFloor) ||
                    (direction == Direction.DOWN && requestFloor <= currentFloor)) {
                    directionPenalty = 0;  // On the way
                } else {
                    directionPenalty = 20;  // Same direction but already passed
                }
            } else {
                directionPenalty = 20;  // Moving in opposite direction
            }

            // Load penalty (number of pending stops)
            int loadPenalty = upStops.size() + downStops.size();

            return distance + directionPenalty + loadPenalty;
        } finally {
            lock.unlock();
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getCurrentFloor() {
        lock.lock();
        try {
            return currentFloor;
        } finally {
            lock.unlock();
        }
    }

    public Direction getDirection() {
        lock.lock();
        try {
            return direction;
        } finally {
            lock.unlock();
        }
    }

    public ElevatorState getState() {
        lock.lock();
        try {
            return state;
        } finally {
            lock.unlock();
        }
    }

    public int getPendingStops() {
        lock.lock();
        try {
            return upStops.size() + downStops.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            return String.format("Elevator-%d [Floor=%d, State=%s, Dir=%s, Pending=%d (↑%d,↓%d)]",
                    id, currentFloor, state, direction,
                    upStops.size() + downStops.size(), upStops.size(), downStops.size());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get detailed status with pending stops
     */
    public String getDetailedStatus() {
        lock.lock();
        try {
            return String.format("Elevator-%d [Floor=%d, State=%s, Dir=%s, UpStops=%s, DownStops=%s]",
                    id, currentFloor, state, direction, upStops, downStops);
        } finally {
            lock.unlock();
        }
    }
}
