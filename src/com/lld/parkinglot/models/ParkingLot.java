package com.lld.parkinglot.models;

import com.lld.parkinglot.strategies.FeeStrategy;
import com.lld.parkinglot.strategies.HourlyFeeStrategy;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main Parking Lot system
 *
 * Features:
 * - Multiple floors
 * - Thread-safe vehicle parking/exit
 * - Pluggable fee calculation strategy
 * - Prevents double allocation
 * - Graceful handling when full
 */
public class ParkingLot {
    private final String parkingLotId;
    private final List<Floor> floors;
    private final Map<String, Ticket> activeTickets;  // ticketId -> Ticket
    private FeeStrategy feeStrategy;

    public ParkingLot(String parkingLotId, int numFloors) {
        this.parkingLotId = parkingLotId;
        this.floors = new ArrayList<>();
        this.activeTickets = new ConcurrentHashMap<>();
        this.feeStrategy = new HourlyFeeStrategy();  // Default strategy

        // Initialize floors
        for (int i = 1; i <= numFloors; i++) {
            floors.add(new Floor(i));
        }
    }

    /**
     * Park a vehicle - assign nearest available compatible spot
     *
     * Algorithm:
     * 1. Check floors in order (1, 2, 3, ...)
     * 2. On each floor, try to get available spot
     * 3. If spot found, reserve atomically and issue ticket
     * 4. If no spot found on any floor, return null (full)
     *
     * Thread-safe: Uses atomic reservation in ParkingSpot
     */
    public Ticket parkVehicle(Vehicle vehicle) {
        // Check floors in order (nearest first)
        for (Floor floor : floors) {
            ParkingSpot spot = floor.getAvailableSpot(vehicle);

            if (spot != null) {
                // Spot found and reserved!
                Ticket ticket = new Ticket(vehicle, spot);
                activeTickets.put(ticket.getTicketId(), ticket);

                System.out.println("✓ Parked " + vehicle + " at " + spot.getSpotId() +
                                 " (Floor-" + floor.getFloorNumber() + ")");
                return ticket;
            }
        }

        // No spot available on any floor
        System.out.println("✗ Parking FULL - Cannot park " + vehicle);
        return null;
    }

    /**
     * Exit vehicle - calculate fee and free spot
     *
     * Returns: parking fee
     * Throws: IllegalArgumentException if ticket not found
     */
    public double exitVehicle(String ticketId) {
        Ticket ticket = activeTickets.remove(ticketId);

        if (ticket == null) {
            throw new IllegalArgumentException("Ticket not found: " + ticketId);
        }

        // Set exit time
        ticket.setExitTime(LocalDateTime.now());

        // Calculate parking duration
        Duration duration = Duration.between(ticket.getEntryTime(), ticket.getExitTime());

        // Calculate fee using strategy
        double fee = feeStrategy.calculateFee(ticket, duration);
        ticket.setFee(fee);

        // Free the spot
        ParkingSpot spot = ticket.getSpot();
        Floor floor = floors.get(spot.getFloorNumber() - 1);
        floor.freeSpot(spot);

        System.out.println("✓ " + ticket.getVehicle() + " exited from " + spot.getSpotId() +
                         " | Duration: " + formatDuration(duration) +
                         " | Fee: $" + String.format("%.2f", fee));

        return fee;
    }

    /**
     * Set fee calculation strategy (Strategy Pattern)
     */
    public void setFeeStrategy(FeeStrategy feeStrategy) {
        this.feeStrategy = feeStrategy;
        System.out.println("✓ Fee strategy changed to: " + feeStrategy.getStrategyName());
    }

    /**
     * Get floor by number (1-indexed)
     */
    public Floor getFloor(int floorNumber) {
        if (floorNumber < 1 || floorNumber > floors.size()) {
            throw new IllegalArgumentException("Invalid floor number: " + floorNumber);
        }
        return floors.get(floorNumber - 1);
    }

    /**
     * Display current status of parking lot
     */
    public void displayStatus() {
        System.out.println("\n=== Parking Lot Status ===");
        System.out.println("Parking Lot: " + parkingLotId);
        System.out.println("Total Floors: " + floors.size());
        System.out.println("Active Vehicles: " + activeTickets.size());
        System.out.println("\nFloor-wise Status:");

        for (Floor floor : floors) {
            System.out.println("  " + floor);
        }

        System.out.println("\nFee Strategy: " + feeStrategy.getStrategyName());
        System.out.println("=========================\n");
    }

    // Helper method to format duration
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours + "h " + minutes + "m";
    }

    public List<Floor> getFloors() {
        return floors;
    }

    public int getActiveVehicleCount() {
        return activeTickets.size();
    }
}
