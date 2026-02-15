package com.lld.airbnb.strategies;

import com.lld.airbnb.models.Booking;

/**
 * Strategy Pattern: Interface for different cancellation policies
 *
 * This allows us to add new cancellation policies without modifying existing code
 * (Open/Closed Principle)
 */
public interface CancellationStrategy {
    /**
     * Calculate refund amount based on the specific cancellation policy
     *
     * @param booking The booking being cancelled
     * @param daysUntilCheckIn Days remaining until check-in date
     * @return Refund amount in dollars
     */
    double calculateRefund(Booking booking, long daysUntilCheckIn);

    /**
     * Get the name of this cancellation policy
     */
    String getPolicyName();
}
