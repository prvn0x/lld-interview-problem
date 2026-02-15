package com.lld.parkinglot.models;

import com.lld.parkinglot.enums.SpotSize;
import com.lld.parkinglot.enums.SpotStatus;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe parking spot with atomic reservation
 */
public class ParkingSpot {
    private final String spotId;
    private final SpotSize size;
    private final int floorNumber;
    private SpotStatus status;
    private Vehicle currentVehicle;
    private final ReentrantLock lock;  // For thread-safe operations

    public ParkingSpot(String spotId, SpotSize size, int floorNumber) {
        this.spotId = spotId;
        this.size = size;
        this.floorNumber = floorNumber;
        this.status = SpotStatus.AVAILABLE;
        this.lock = new ReentrantLock();
    }

    /**
     * Check if this spot can fit the given vehicle
     * Vehicle can park only if spot size >= vehicle size
     */
    public boolean canFit(Vehicle vehicle) {
        return size.canFit(vehicle.getType().getRequiredSize());
    }

    /**
     * Atomically reserve this spot for a vehicle
     * Returns true if reservation successful, false otherwise
     *
     * Thread-safe using ReentrantLock
     */
    public boolean reserve(Vehicle vehicle) {
        lock.lock();
        try {
            if (status == SpotStatus.AVAILABLE && canFit(vehicle)) {
                status = SpotStatus.OCCUPIED;
                currentVehicle = vehicle;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Vacate the spot (make it available again)
     * Thread-safe
     */
    public void vacate() {
        lock.lock();
        try {
            status = SpotStatus.AVAILABLE;
            currentVehicle = null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Check if spot is currently available
     * Thread-safe
     */
    public boolean isAvailable() {
        lock.lock();
        try {
            return status == SpotStatus.AVAILABLE;
        } finally {
            lock.unlock();
        }
    }

    // Getters
    public String getSpotId() { return spotId; }
    public SpotSize getSize() { return size; }
    public int getFloorNumber() { return floorNumber; }
    public SpotStatus getStatus() { return status; }
    public Vehicle getCurrentVehicle() { return currentVehicle; }

    @Override
    public String toString() {
        return "Spot{" + spotId + ", " + size + ", Floor-" + floorNumber +
               ", " + status + "}";
    }
}
