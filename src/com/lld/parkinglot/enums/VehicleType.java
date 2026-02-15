package com.lld.parkinglot.enums;

public enum VehicleType {
    BIKE(SpotSize.SMALL),
    CAR(SpotSize.MEDIUM),
    TRUCK(SpotSize.LARGE);

    private final SpotSize requiredSize;

    VehicleType(SpotSize requiredSize) {
        this.requiredSize = requiredSize;
    }

    public SpotSize getRequiredSize() {
        return requiredSize;
    }
}
