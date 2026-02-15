package com.lld.airbnb.strategies;

import com.lld.airbnb.models.Booking;

/**
 * Moderate Cancellation Policy:
 * - Full refund if cancelled 5+ days before check-in
 * - 50% refund if cancelled 1-4 days before check-in
 * - No refund if cancelled within 24 hours of check-in
 */
public class ModerateCancellationStrategy implements CancellationStrategy {

    @Override
    public double calculateRefund(Booking booking, long daysUntilCheckIn) {
        double totalAmount = booking.getTotalAmount();

        if (daysUntilCheckIn >= 5) {
            // Full refund if cancelled 5+ days before check-in
            return totalAmount;
        } else if (daysUntilCheckIn >= 1) {
            // 50% refund if cancelled 1-4 days before check-in
            return totalAmount * 0.5;
        } else {
            // No refund if cancelled within 24 hours
            return 0;
        }
    }

    @Override
    public String getPolicyName() {
        return "MODERATE";
    }

    @Override
    public String toString() {
        return "ModerateCancellation: Full refund 5+ days, 50% refund 1-4 days, No refund <24 hours";
    }
}
