package com.lld.elevator.enums;

/**
 * Elevator State Machine states
 */
public enum ElevatorState {
    IDLE,           // Not moving, no pending requests
    MOVING_UP,      // Moving upward
    MOVING_DOWN,    // Moving downward
    DOOR_OPEN       // Stopped at a floor with door open
}
