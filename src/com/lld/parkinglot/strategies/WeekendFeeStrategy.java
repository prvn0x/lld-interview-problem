package com.lld.parkinglot.strategies;

import com.lld.parkinglot.models.Ticket;
import java.time.DayOfWeek;
import java.time.Duration;

/**
 * Weekend fee strategy - 1.5x higher rates on weekends
 */
public class WeekendFeeStrategy implements FeeStrategy {
    private final HourlyFeeStrategy baseStrategy;
    private static final double WEEKEND_MULTIPLIER = 1.5;

    public WeekendFeeStrategy() {
        this.baseStrategy = new HourlyFeeStrategy();
    }

    @Override
    public double calculateFee(Ticket ticket, Duration parkingDuration) {
        double baseFee = baseStrategy.calculateFee(ticket, parkingDuration);

        // Check if entry time was on weekend
        DayOfWeek dayOfWeek = ticket.getEntryTime().getDayOfWeek();
        boolean isWeekend = (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);

        if (isWeekend) {
            return baseFee * WEEKEND_MULTIPLIER;
        }

        return baseFee;
    }

    @Override
    public String getStrategyName() {
        return "Weekend Fee Strategy (1.5x on weekends)";
    }
}
