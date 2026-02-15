package com.lld.parkinglot.strategies;

import com.lld.parkinglot.models.Ticket;
import java.time.Duration;

/**
 * Strategy Pattern for parking fee calculation
 *
 * Different strategies:
 * - HourlyFeeStrategy (default)
 * - WeekendFeeStrategy (higher rates on weekends)
 * - SurgeFeeStrategy (dynamic pricing)
 */
public interface FeeStrategy {
    double calculateFee(Ticket ticket, Duration parkingDuration);
    String getStrategyName();
}
