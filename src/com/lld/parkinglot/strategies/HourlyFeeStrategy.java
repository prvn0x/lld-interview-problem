package com.lld.parkinglot.strategies;

import com.lld.parkinglot.enums.VehicleType;
import com.lld.parkinglot.models.Ticket;
import java.time.Duration;

/**
 * Hourly fee calculation strategy
 *
 * Rates:
 * - Bike: $2/hour
 * - Car: $5/hour
 * - Truck: $10/hour
 */
public class HourlyFeeStrategy implements FeeStrategy {
    private static final double BIKE_RATE = 2.0;
    private static final double CAR_RATE = 5.0;
    private static final double TRUCK_RATE = 10.0;

    @Override
    public double calculateFee(Ticket ticket, Duration parkingDuration) {
        // Calculate hours (round up)
        long hours = parkingDuration.toHours();
        if (parkingDuration.toMinutesPart() > 0) {
            hours++;  // Round up for partial hour
        }

        if (hours == 0) {
            hours = 1;  // Minimum 1 hour charge
        }

        double hourlyRate = getHourlyRate(ticket.getVehicle().getType());
        return hours * hourlyRate;
    }

    private double getHourlyRate(VehicleType vehicleType) {
        switch (vehicleType) {
            case BIKE:
                return BIKE_RATE;
            case CAR:
                return CAR_RATE;
            case TRUCK:
                return TRUCK_RATE;
            default:
                return CAR_RATE;
        }
    }

    @Override
    public String getStrategyName() {
        return "Hourly Fee Strategy";
    }
}
