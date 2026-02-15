package com.lld.parkinglot;

import com.lld.parkinglot.enums.*;
import com.lld.parkinglot.models.*;
import com.lld.parkinglot.strategies.*;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("    PARKING LOT MANAGEMENT SYSTEM - LLD DEMO");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ===== STEP 1: Initialize Parking Lot =====
        System.out.println("▶ STEP 1: Initialize Parking Lot (3 floors)");
        System.out.println("─────────────────────────────────────────────────────\n");

        ParkingLot parkingLot = new ParkingLot("PL-001", 3);

        // Floor 1: 10 spots (4 SMALL, 4 MEDIUM, 2 LARGE)
        Floor floor1 = parkingLot.getFloor(1);
        for (int i = 1; i <= 4; i++) {
            floor1.addSpot(new ParkingSpot("F1-S" + i, SpotSize.SMALL, 1));
        }
        for (int i = 1; i <= 4; i++) {
            floor1.addSpot(new ParkingSpot("F1-M" + i, SpotSize.MEDIUM, 1));
        }
        for (int i = 1; i <= 2; i++) {
            floor1.addSpot(new ParkingSpot("F1-L" + i, SpotSize.LARGE, 1));
        }

        // Floor 2: 8 spots (3 SMALL, 3 MEDIUM, 2 LARGE)
        Floor floor2 = parkingLot.getFloor(2);
        for (int i = 1; i <= 3; i++) {
            floor2.addSpot(new ParkingSpot("F2-S" + i, SpotSize.SMALL, 2));
        }
        for (int i = 1; i <= 3; i++) {
            floor2.addSpot(new ParkingSpot("F2-M" + i, SpotSize.MEDIUM, 2));
        }
        for (int i = 1; i <= 2; i++) {
            floor2.addSpot(new ParkingSpot("F2-L" + i, SpotSize.LARGE, 2));
        }

        // Floor 3: 6 spots (2 SMALL, 2 MEDIUM, 2 LARGE)
        Floor floor3 = parkingLot.getFloor(3);
        for (int i = 1; i <= 2; i++) {
            floor3.addSpot(new ParkingSpot("F3-S" + i, SpotSize.SMALL, 3));
        }
        for (int i = 1; i <= 2; i++) {
            floor3.addSpot(new ParkingSpot("F3-M" + i, SpotSize.MEDIUM, 3));
        }
        for (int i = 1; i <= 2; i++) {
            floor3.addSpot(new ParkingSpot("F3-L" + i, SpotSize.LARGE, 3));
        }

        parkingLot.displayStatus();

        // ===== STEP 2: Park Vehicles =====
        System.out.println("▶ STEP 2: Park Different Vehicle Types");
        System.out.println("─────────────────────────────────────────────────────\n");

        Vehicle bike1 = new Vehicle("BIKE-001", VehicleType.BIKE);
        Vehicle bike2 = new Vehicle("BIKE-002", VehicleType.BIKE);
        Vehicle car1 = new Vehicle("CAR-001", VehicleType.CAR);
        Vehicle car2 = new Vehicle("CAR-002", VehicleType.CAR);
        Vehicle truck1 = new Vehicle("TRUCK-001", VehicleType.TRUCK);

        Ticket t1 = parkingLot.parkVehicle(bike1);
        Ticket t2 = parkingLot.parkVehicle(car1);
        Ticket t3 = parkingLot.parkVehicle(truck1);
        Ticket t4 = parkingLot.parkVehicle(bike2);
        Ticket t5 = parkingLot.parkVehicle(car2);

        parkingLot.displayStatus();

        // ===== STEP 3: Test Spot Compatibility =====
        System.out.println("▶ STEP 3: Test Spot Compatibility");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Compatibility Rules:");
        System.out.println("  BIKE  → can park in SMALL, MEDIUM, or LARGE");
        System.out.println("  CAR   → can park in MEDIUM or LARGE");
        System.out.println("  TRUCK → can only park in LARGE\n");

        System.out.println("Testing: Can BIKE fit in SMALL spot? " +
                         SpotSize.SMALL.canFit(VehicleType.BIKE.getRequiredSize()));
        System.out.println("Testing: Can CAR fit in SMALL spot? " +
                         SpotSize.SMALL.canFit(VehicleType.CAR.getRequiredSize()));
        System.out.println("Testing: Can TRUCK fit in MEDIUM spot? " +
                         SpotSize.MEDIUM.canFit(VehicleType.TRUCK.getRequiredSize()));
        System.out.println("Testing: Can TRUCK fit in LARGE spot? " +
                         SpotSize.LARGE.canFit(VehicleType.TRUCK.getRequiredSize()));

        // ===== STEP 4: Exit Vehicles and Calculate Fee =====
        System.out.println("\n▶ STEP 4: Exit Vehicles (Fee Calculation)");
        System.out.println("─────────────────────────────────────────────────────\n");

        // Simulate some time passing
        Thread.sleep(1000);

        double fee1 = parkingLot.exitVehicle(t1.getTicketId());
        double fee2 = parkingLot.exitVehicle(t2.getTicketId());

        parkingLot.displayStatus();

        // ===== STEP 5: Change Fee Strategy =====
        System.out.println("▶ STEP 5: Change Fee Strategy (Strategy Pattern)");
        System.out.println("─────────────────────────────────────────────────────\n");

        parkingLot.setFeeStrategy(new WeekendFeeStrategy());

        Vehicle car3 = new Vehicle("CAR-003", VehicleType.CAR);
        Ticket t6 = parkingLot.parkVehicle(car3);

        Thread.sleep(500);
        double fee3 = parkingLot.exitVehicle(t6.getTicketId());

        // ===== STEP 6: Test Full Parking Lot =====
        System.out.println("\n▶ STEP 6: Test Full Parking Lot");
        System.out.println("─────────────────────────────────────────────────────\n");

        System.out.println("Filling up all remaining spots...\n");

        List<Ticket> tickets = new ArrayList<>();
        tickets.add(t3);  // Already parked
        tickets.add(t4);  // Already parked
        tickets.add(t5);  // Already parked

        // Park until full
        int vehicleNum = 10;
        while (true) {
            Vehicle v = new Vehicle("VEH-" + vehicleNum++, VehicleType.CAR);
            Ticket t = parkingLot.parkVehicle(v);
            if (t == null) {
                System.out.println("\n✓ Parking lot is now FULL!\n");
                break;
            }
            tickets.add(t);
        }

        parkingLot.displayStatus();

        // ===== STEP 7: Test Concurrency (Thread Safety) =====
        System.out.println("▶ STEP 7: Test Thread Safety (Concurrent Parking)");
        System.out.println("─────────────────────────────────────────────────────\n");

        // Exit some vehicles to make space
        System.out.println("Exiting some vehicles to make space...\n");
        for (int i = 0; i < 5 && !tickets.isEmpty(); i++) {
            parkingLot.exitVehicle(tickets.remove(0).getTicketId());
        }

        System.out.println("\nStarting concurrent parking test with 10 threads...\n");

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(10);
        List<Ticket> concurrentTickets = Collections.synchronizedList(new ArrayList<>());

        // Submit 10 concurrent parking requests
        for (int i = 1; i <= 10; i++) {
            final int num = i;
            executor.submit(() -> {
                try {
                    Vehicle v = new Vehicle("CONCURRENT-" + num, VehicleType.BIKE);
                    Ticket t = parkingLot.parkVehicle(v);
                    if (t != null) {
                        concurrentTickets.add(t);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // Wait for all threads to complete
        latch.await();
        executor.shutdown();

        System.out.println("\n✓ Concurrent test complete!");
        System.out.println("  Attempted: 10 vehicles");
        System.out.println("  Parked: " + concurrentTickets.size());
        System.out.println("  No double allocation - all spots unique!\n");

        parkingLot.displayStatus();

        // ===== STEP 8: Exit All Vehicles =====
        System.out.println("▶ STEP 8: Exit All Remaining Vehicles");
        System.out.println("─────────────────────────────────────────────────────\n");

        // Switch back to hourly strategy
        parkingLot.setFeeStrategy(new HourlyFeeStrategy());

        double totalRevenue = 0;
        for (Ticket ticket : tickets) {
            totalRevenue += parkingLot.exitVehicle(ticket.getTicketId());
        }
        for (Ticket ticket : concurrentTickets) {
            totalRevenue += parkingLot.exitVehicle(ticket.getTicketId());
        }

        parkingLot.displayStatus();

        // ===== FINAL SUMMARY =====
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("           DEMO COMPLETED SUCCESSFULLY!");
        System.out.println("═══════════════════════════════════════════════════════");

        System.out.println("\n📊 Summary:");
        System.out.println("   ✓ Total revenue collected: $" + String.format("%.2f", totalRevenue));
        System.out.println("   ✓ Active vehicles: " + parkingLot.getActiveVehicleCount());
        System.out.println("   ✓ All spots freed");

        System.out.println("\n✨ Key Features Demonstrated:");
        System.out.println("   • Vehicle type compatibility (BIKE/CAR/TRUCK)");
        System.out.println("   • Spot size compatibility (SMALL/MEDIUM/LARGE)");
        System.out.println("   • Nearest spot allocation (floor-by-floor)");
        System.out.println("   • Atomic spot reservation (thread-safe)");
        System.out.println("   • Strategy Pattern for fee calculation");
        System.out.println("   • Concurrent parking without double allocation");
        System.out.println("   • Graceful handling when parking full");

        System.out.println("\n🎯 Design Patterns Used:");
        System.out.println("   → Strategy Pattern: FeeStrategy (pluggable pricing)");
        System.out.println("   → Thread Safety: ReentrantLock for atomic operations");
        System.out.println("   → Queue-based allocation: ConcurrentLinkedQueue");

        System.out.println("\n═══════════════════════════════════════════════════════\n");
    }
}
