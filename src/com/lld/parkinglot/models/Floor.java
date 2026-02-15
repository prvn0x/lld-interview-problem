package com.lld.parkinglot.models;

import com.lld.parkinglot.enums.SpotSize;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Floor in the parking lot
 *
 * Each floor maintains:
 * - Available spots by size (for fast allocation)
 * - All spots map (for management)
 *
 * Thread-safe operations using concurrent collections
 */
public class Floor {
    private final int floorNumber;
    private final Map<String, ParkingSpot> allSpots;  // spotId -> ParkingSpot
    private final Map<SpotSize, Queue<ParkingSpot>> availableSpotsBySize;

    public Floor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.allSpots = new ConcurrentHashMap<>();
        this.availableSpotsBySize = new ConcurrentHashMap<>();

        // Initialize queues for each spot size
        for (SpotSize size : SpotSize.values()) {
            availableSpotsBySize.put(size, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Add a parking spot to this floor
     */
    public void addSpot(ParkingSpot spot) {
        allSpots.put(spot.getSpotId(), spot);
        availableSpotsBySize.get(spot.getSize()).offer(spot);
    }

    /**
     * Get an available spot for the given vehicle
     * Returns null if no compatible spot available
     *
     * Strategy: Check spots in size order (SMALL -> MEDIUM -> LARGE)
     * and return first compatible available spot
     */
    public ParkingSpot getAvailableSpot(Vehicle vehicle) {
        SpotSize requiredSize = vehicle.getType().getRequiredSize();

        // Try spots of required size first, then larger sizes
        for (SpotSize size : SpotSize.values()) {
            if (size.canFit(requiredSize)) {
                Queue<ParkingSpot> queue = availableSpotsBySize.get(size);

                // Try to get a spot from queue
                ParkingSpot spot = queue.poll();
                while (spot != null) {
                    // Try to reserve atomically
                    if (spot.reserve(vehicle)) {
                        return spot;  // Success!
                    }
                    // Failed (race condition) - try next spot
                    spot = queue.poll();
                }
            }
        }

        return null;  // No available spot found
    }

    /**
     * Free a parking spot (make it available again)
     */
    public void freeSpot(ParkingSpot spot) {
        spot.vacate();
        availableSpotsBySize.get(spot.getSize()).offer(spot);
    }

    /**
     * Get count of available spots by size
     */
    public int getAvailableCount(SpotSize size) {
        return availableSpotsBySize.get(size).size();
    }

    /**
     * Get total available spots on this floor
     */
    public int getTotalAvailableSpots() {
        return availableSpotsBySize.values().stream()
                .mapToInt(Queue::size)
                .sum();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public Map<String, ParkingSpot> getAllSpots() {
        return allSpots;
    }

    @Override
    public String toString() {
        return "Floor-" + floorNumber +
               " (Total: " + allSpots.size() +
               ", Available: " + getTotalAvailableSpots() + ")";
    }
}
