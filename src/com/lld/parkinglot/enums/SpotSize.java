package com.lld.parkinglot.enums;

public enum SpotSize {
    SMALL(1),      // For bikes
    MEDIUM(2),     // For cars
    LARGE(3);      // For trucks

    private final int value;

    SpotSize(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    // Check if this spot size can accommodate a vehicle size
    public boolean canFit(SpotSize vehicleSize) {
        return this.value >= vehicleSize.value;
    }
}
