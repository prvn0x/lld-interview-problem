package com.lld.airbnb.strategies;

import com.lld.airbnb.models.Booking;

/**
 * Flexible Cancellation Policy:
 * - Full refund if cancelled 24 hours (1 day) before check-in
 * - 50% refund if cancelled within 24 hours of check-in
 */
public class FlexibleCancellationStrategy implements CancellationStrategy {

    @Override
    public double calculateRefund(Booking booking, long daysUntilCheckIn) {
        double totalAmount = booking.getTotalAmount();

        if (daysUntilCheckIn >= 1) {
            // Full refund if cancelled 1+ days before check-in
            return totalAmount;
        } else {
            // 50% refund if cancelled within 24 hours
            return totalAmount * 0.5;
        }
    }

    @Override
    public String getPolicyName() {
        return "FLEXIBLE";
    }

    @Override
    public String toString() {
        return "FlexibleCancellation: Full refund if cancelled 1+ days before check-in, 50% within 24 hours";
    }
}
