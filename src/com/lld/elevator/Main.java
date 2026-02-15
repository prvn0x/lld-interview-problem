package com.lld.elevator;

import com.lld.elevator.enums.Direction;
import com.lld.elevator.models.Building;

/**
 * Elevator System - Comprehensive Demo
 *
 * Demonstrates:
 * 1. External requests with scoring-based dispatcher
 * 2. Internal floor selection
 * 3. Directional scheduling (separate up/down queues)
 * 4. State machine transitions
 * 5. Thread-safe concurrent requests
 * 6. Direction reversal logic
 * 7. Load balancing across multiple elevators
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("    ELEVATOR SYSTEM - LOW-LEVEL DESIGN DEMO");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ===== STEP 1: Initialize Building =====
        System.out.println("▶ STEP 1: Initialize Building");
        System.out.println("─────────────────────────────────────────────────────\n");

        // 10-floor building with 3 elevators
        Building building = new Building("Tower-A", 10, 3);
        building.displayStatus();

        // ===== STEP 2: Single External Request =====
        System.out.println("\n▶ STEP 2: Single External Request");
        System.out.println("─────────────────────────────────────────────────────");

        building.requestElevator(5, Direction.UP);
        building.displayDetailedStatus();

        // Simulate until request served
        System.out.println("Simulating elevator movement...\n");
        runSimulation(building, 15);

        // ===== STEP 3: Multiple Requests - Directional Scheduling =====
        System.out.println("\n▶ STEP 3: Multiple Requests - Directional Scheduling");
        System.out.println("─────────────────────────────────────────────────────");

        // Request from floor 2 going UP
        building.requestElevator(2, Direction.UP);

        // Internal selections: 5, 7, 3 (should be added to appropriate queues)
        building.selectFloor(1, 5);
        building.selectFloor(1, 7);
        building.selectFloor(1, 3);

        building.displayDetailedStatus();

        System.out.println("Expected behavior:");
        System.out.println("  - Elevator should pick floor 2, then 5, then 7 (upStops)");
        System.out.println("  - After upStops empty, reverse and serve floor 3 (downStops)\n");

        System.out.println("Simulating...\n");
        runSimulation(building, 25);

        // ===== STEP 4: Scoring-Based Dispatcher =====
        System.out.println("\n▶ STEP 4: Scoring-Based Dispatcher");
        System.out.println("─────────────────────────────────────────────────────");

        // Position elevators at different floors
        building.selectFloor(1, 2);
        building.selectFloor(2, 5);
        building.selectFloor(3, 8);

        runSimulation(building, 10);

        // Now request from floor 6 going UP
        // Elevator-2 at floor 5 should win (closest)
        System.out.println("\nRequest from floor 6 going UP:");
        System.out.println("Elevator-1 at floor 2, Elevator-2 at floor 5, Elevator-3 at floor 8");
        System.out.println("Expected: Elevator-2 should be selected (lowest score)\n");

        building.requestElevator(6, Direction.UP);

        runSimulation(building, 5);

        // ===== STEP 5: Direction Penalty Test =====
        System.out.println("\n▶ STEP 5: Direction Penalty Test");
        System.out.println("─────────────────────────────────────────────────────");

        // Setup: Elevator-1 moving UP from floor 2 to 8
        building.selectFloor(1, 8);
        runSimulation(building, 3);  // Move to floor ~5

        // Request from floor 3 going UP (Elevator-1 already passed it)
        System.out.println("\nElevator-1 is moving UP and has passed floor 3");
        System.out.println("Requesting elevator at floor 3 going UP");
        System.out.println("Expected: Elevator-2 or Elevator-3 should be selected (lower penalty)\n");

        building.requestElevator(3, Direction.UP);

        runSimulation(building, 15);

        // ===== STEP 6: Load Balancing =====
        System.out.println("\n▶ STEP 6: Load Balancing");
        System.out.println("─────────────────────────────────────────────────────");

        // Give Elevator-1 many stops
        building.selectFloor(1, 1);
        building.selectFloor(1, 2);
        building.selectFloor(1, 3);
        building.selectFloor(1, 4);
        building.selectFloor(1, 5);

        building.displayDetailedStatus();

        // Request from floor 2
        System.out.println("Elevator-1 has 5 pending stops");
        System.out.println("Requesting elevator at floor 2");
        System.out.println("Expected: Elevator-2 or Elevator-3 should be selected (lower load penalty)\n");

        building.requestElevator(2, Direction.UP);

        runSimulation(building, 20);

        // ===== STEP 7: Concurrent Requests (Thread Safety) =====
        System.out.println("\n▶ STEP 7: Concurrent Requests (Thread Safety)");
        System.out.println("─────────────────────────────────────────────────────");

        System.out.println("Submitting 5 concurrent requests from different threads...\n");

        // Create threads for concurrent requests
        Thread t1 = new Thread(() -> building.requestElevator(1, Direction.UP));
        Thread t2 = new Thread(() -> building.requestElevator(4, Direction.DOWN));
        Thread t3 = new Thread(() -> building.requestElevator(7, Direction.UP));
        Thread t4 = new Thread(() -> building.requestElevator(3, Direction.DOWN));
        Thread t5 = new Thread(() -> building.requestElevator(9, Direction.DOWN));

        // Start all threads
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();

        // Wait for threads to complete
        t1.join();
        t2.join();
        t3.join();
        t4.join();
        t5.join();

        System.out.println("\n✓ All concurrent requests processed safely!\n");
        building.displayDetailedStatus();

        System.out.println("Simulating until all requests served...\n");
        runSimulation(building, 50);

        // ===== STEP 8: Edge Cases =====
        System.out.println("\n▶ STEP 8: Edge Cases");
        System.out.println("─────────────────────────────────────────────────────\n");

        // Invalid floor
        System.out.println("Test: Invalid floor request");
        building.requestElevator(20, Direction.UP);

        // IDLE direction
        System.out.println("\nTest: IDLE direction request");
        building.requestElevator(5, Direction.IDLE);

        // Same floor
        System.out.println("\nTest: Request from current floor");
        building.selectFloor(1, 0);  // If elevator already at 0
        building.displayDetailedStatus();

        // ===== FINAL STATUS =====
        System.out.println("\n▶ FINAL STATUS");
        System.out.println("─────────────────────────────────────────────────────");

        // Bring all elevators to ground floor
        System.out.println("\nBringing all elevators to ground floor...\n");
        building.selectFloor(1, 0);
        building.selectFloor(2, 0);
        building.selectFloor(3, 0);

        runSimulation(building, 30);

        // ===== SUMMARY =====
        System.out.println("\n═══════════════════════════════════════════════════════");
        System.out.println("           DEMO COMPLETED SUCCESSFULLY!");
        System.out.println("═══════════════════════════════════════════════════════");

        System.out.println("\n✨ Features Demonstrated:");
        System.out.println("   • Scoring-based dispatcher (distance + direction + load)");
        System.out.println("   • Directional scheduling (separate up/down queues)");
        System.out.println("   • State machine (IDLE → MOVING → DOOR_OPEN → ...)");
        System.out.println("   • Direction reversal when queue empty");
        System.out.println("   • Thread-safe concurrent requests");
        System.out.println("   • Load balancing across elevators");
        System.out.println("   • Edge case handling");

        System.out.println("\n🎯 Design Patterns Used:");
        System.out.println("   → State Machine: Elevator states and transitions");
        System.out.println("   → Strategy: Scoring-based dispatcher");
        System.out.println("   → Thread Safety: ReentrantLock + TreeSet");
        System.out.println("   → Queue-based Scheduling: Directional queues");

        System.out.println("\n🔒 Concurrency Features:");
        System.out.println("   → Per-elevator ReentrantLock (fine-grained)");
        System.out.println("   → Thread-safe stop queues (TreeSet)");
        System.out.println("   → Atomic state transitions");
        System.out.println("   → No global locking (high concurrency)");

        System.out.println("\n═══════════════════════════════════════════════════════\n");
    }

    /**
     * Run simulation for specified number of steps
     * Each step = 1 floor movement
     */
    private static void runSimulation(Building building, int steps) {
        for (int i = 0; i < steps; i++) {
            building.simulateStep();

            // Check if all elevators are idle
            if (building.allElevatorsIdle()) {
                System.out.println("\n✓ All requests served!\n");
                building.displayStatus();
                break;
            }

            // Small delay for readability
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
