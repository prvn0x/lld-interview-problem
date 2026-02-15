package com.lld.elevator.models;

import com.lld.elevator.enums.Direction;

/**
 * Represents an elevator request
 * Can be:
 * - External request: user at floor requesting elevator with direction
 * - Internal request: user inside elevator selecting destination
 */
public class Request {
    private final int sourceFloor;
    private final Direction direction;
    private final Integer destinationFloor;  // Optional for external requests

    // External request (from floor panel)
    public Request(int sourceFloor, Direction direction) {
        this.sourceFloor = sourceFloor;
        this.direction = direction;
        this.destinationFloor = null;
    }

    // Internal request (from inside elevator)
    public Request(int sourceFloor, Direction direction, int destinationFloor) {
        this.sourceFloor = sourceFloor;
        this.direction = direction;
        this.destinationFloor = destinationFloor;
    }

    public int getSourceFloor() {
        return sourceFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public Integer getDestinationFloor() {
        return destinationFloor;
    }

    @Override
    public String toString() {
        if (destinationFloor != null) {
            return String.format("Request{floor=%d, dir=%s, dest=%d}",
                    sourceFloor, direction, destinationFloor);
        } else {
            return String.format("Request{floor=%d, dir=%s}",
                    sourceFloor, direction);
        }
    }
}
