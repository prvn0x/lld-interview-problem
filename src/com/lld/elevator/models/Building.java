package com.lld.elevator.models;

import com.lld.elevator.enums.Direction;
import com.lld.elevator.services.ElevatorController;

import java.util.ArrayList;
import java.util.List;

/**
 * Building class manages all elevators
 *
 * Public APIs:
 * - requestElevator(floor, direction): Request elevator from a floor
 * - selectFloor(elevatorId, destination): Select destination from inside elevator
 */
public class Building {
    private final String buildingId;
    private final int totalFloors;
    private final List<Elevator> elevators;
    private final ElevatorController controller;

    public Building(String buildingId, int totalFloors, int numElevators) {
        this.buildingId = buildingId;
        this.totalFloors = totalFloors;
        this.elevators = new ArrayList<>();
        this.controller = new ElevatorController();

        // Initialize elevators at ground floor
        for (int i = 0; i < numElevators; i++) {
            elevators.add(new Elevator(i + 1, 0));
        }
    }

    /**
     * External request: User at a floor requests elevator with direction
     *
     * Dispatcher selects best elevator using scoring algorithm
     *
     * @param floor Floor number (0 to totalFloors-1)
     * @param direction UP or DOWN
     */
    public void requestElevator(int floor, Direction direction) {
        if (floor < 0 || floor >= totalFloors) {
            System.out.println("✗ Invalid floor: " + floor);
            return;
        }

        if (direction == Direction.IDLE) {
            System.out.println("✗ Cannot request elevator with IDLE direction");
            return;
        }

        System.out.println(String.format("\n▶ Request: Floor %d → %s", floor, direction));
        System.out.println("  Evaluating elevators...");

        // Use dispatcher to select best elevator
        Elevator selectedElevator = controller.selectBestElevator(elevators, floor, direction);

        if (selectedElevator != null) {
            selectedElevator.addStop(floor);
            System.out.println(String.format("  ✓ Elevator-%d assigned to floor %d",
                    selectedElevator.getId(), floor));
        } else {
            System.out.println("  ✗ No elevator available");
        }
    }

    /**
     * Internal request: User inside elevator selects destination floor
     *
     * @param elevatorId Elevator ID
     * @param destination Destination floor
     */
    public void selectFloor(int elevatorId, int destination) {
        if (destination < 0 || destination >= totalFloors) {
            System.out.println("✗ Invalid destination: " + destination);
            return;
        }

        Elevator elevator = getElevatorById(elevatorId);
        if (elevator == null) {
            System.out.println("✗ Elevator not found: " + elevatorId);
            return;
        }

        System.out.println(String.format("\n▶ Elevator-%d: Internal selection → Floor %d",
                elevatorId, destination));

        elevator.addStop(destination);
        System.out.println(String.format("  ✓ Floor %d added to Elevator-%d stops", destination, elevatorId));
    }

    /**
     * Simulate one time step for all elevators
     * Each elevator moves one floor if in motion
     */
    public void simulateStep() {
        for (Elevator elevator : elevators) {
            elevator.moveOneStep();
        }
    }

    /**
     * Display current status of all elevators
     */
    public void displayStatus() {
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Building Status: " + buildingId);
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        for (Elevator elevator : elevators) {
            System.out.println("  " + elevator.toString());
        }
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
    }

    /**
     * Display detailed status with pending stops
     */
    public void displayDetailedStatus() {
        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.println("Detailed Building Status: " + buildingId);
        System.out.println("═══════════════════════════════════════════════════");
        for (Elevator elevator : elevators) {
            System.out.println("  " + elevator.getDetailedStatus());
        }
        System.out.println("═══════════════════════════════════════════════════\n");
    }

    /**
     * Check if all elevators are idle
     */
    public boolean allElevatorsIdle() {
        for (Elevator elevator : elevators) {
            if (elevator.getPendingStops() > 0) {
                return false;
            }
        }
        return true;
    }

    private Elevator getElevatorById(int id) {
        for (Elevator elevator : elevators) {
            if (elevator.getId() == id) {
                return elevator;
            }
        }
        return null;
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public int getTotalFloors() {
        return totalFloors;
    }
}
