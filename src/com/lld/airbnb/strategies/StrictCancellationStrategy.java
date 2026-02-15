package com.lld.airbnb.strategies;

import com.lld.airbnb.models.Booking;

/**
 * Strict Cancellation Policy:
 * - 50% refund if cancelled 7+ days before check-in
 * - No refund if cancelled within 7 days of check-in
 *
 * This is the most restrictive policy, typically used for high-demand properties
 */
public class StrictCancellationStrategy implements CancellationStrategy {

    @Override
    public double calculateRefund(Booking booking, long daysUntilCheckIn) {
        double totalAmount = booking.getTotalAmount();

        if (daysUntilCheckIn >= 7) {
            // 50% refund if cancelled 7+ days before check-in
            return totalAmount * 0.5;
        } else {
            // No refund if cancelled within 7 days
            return 0;
        }
    }

    @Override
    public String getPolicyName() {
        return "STRICT";
    }

    @Override
    public String toString() {
        return "StrictCancellation: 50% refund if 7+ days before, No refund within 7 days";
    }
}
