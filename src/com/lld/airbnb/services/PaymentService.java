package com.lld.airbnb.services;

import com.lld.airbnb.enums.CancellationPolicy;
import com.lld.airbnb.enums.PaymentStatus;
import com.lld.airbnb.models.Booking;
import com.lld.airbnb.models.Payment;
import com.lld.airbnb.models.Property;
import com.lld.airbnb.strategies.CancellationStrategy;
import com.lld.airbnb.strategies.CancellationStrategyFactory;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class PaymentService {
    private Map<String, Payment> payments;
    private Map<String, Payment> paymentsByBooking;
    private PropertyService propertyService;

    public PaymentService(PropertyService propertyService) {
        this.payments = new HashMap<>();
        this.paymentsByBooking = new HashMap<>();
        this.propertyService = propertyService;
    }

    public Payment processPayment(String paymentId, String bookingId, double amount, String paymentMethod) {
        Payment payment = new Payment(paymentId, bookingId, amount, paymentMethod);

        // Simulate payment processing
        boolean success = payment.processPayment();

        if (success) {
            payments.put(paymentId, payment);
            paymentsByBooking.put(bookingId, payment);
            System.out.println("Payment processed successfully: " + payment);
        } else {
            System.out.println("Payment failed for booking: " + bookingId);
        }

        return payment;
    }

    public Payment getPaymentById(String paymentId) {
        return payments.get(paymentId);
    }

    public Payment getPaymentByBooking(String bookingId) {
        return paymentsByBooking.get(bookingId);
    }

    public void processRefundForBooking(Booking booking) {
        Payment payment = paymentsByBooking.get(booking.getBookingId());
        if (payment == null) {
            System.out.println("No payment found for booking: " + booking.getBookingId());
            return;
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            System.out.println("Cannot refund - payment status: " + payment.getStatus());
            return;
        }

        // Get property's cancellation policy
        Property property = propertyService.getPropertyById(booking.getPropertyId());
        if (property == null) {
            return;
        }

        // ✨ STRATEGY PATTERN: Get the appropriate strategy based on policy
        CancellationPolicy policyType = property.getCancellationPolicy();
        CancellationStrategy strategy = CancellationStrategyFactory.getStrategy(policyType);

        // Calculate refund using the strategy
        double refundAmount = calculateRefundAmount(booking, strategy);

        boolean success = payment.processRefund(refundAmount);
        if (success) {
            System.out.println("Refund processed: $" + refundAmount +
                             " (Policy: " + policyType + " using " + strategy.getPolicyName() + " strategy)");
        }
    }

    /**
     * Calculate refund amount using Strategy Pattern
     *
     * @param booking The booking being cancelled
     * @param strategy The cancellation strategy to use
     * @return Refund amount
     */
    public double calculateRefundAmount(Booking booking, CancellationStrategy strategy) {
        LocalDate today = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(today, booking.getCheckInDate());

        // Delegate to the strategy (Strategy Pattern in action!)
        return strategy.calculateRefund(booking, daysUntilCheckIn);
    }

    /**
     * Legacy method for backward compatibility (if needed)
     * This delegates to the strategy-based method
     */
    public double calculateRefundAmount(Booking booking, CancellationPolicy policy) {
        CancellationStrategy strategy = CancellationStrategyFactory.getStrategy(policy);
        return calculateRefundAmount(booking, strategy);
    }

    public void displayPayment(String paymentId) {
        Payment payment = payments.get(paymentId);
        if (payment != null) {
            System.out.println("\n=== Payment Details ===");
            System.out.println("Payment ID: " + payment.getPaymentId());
            System.out.println("Booking ID: " + payment.getBookingId());
            System.out.println("Amount: $" + payment.getAmount());
            System.out.println("Status: " + payment.getStatus());
            System.out.println("Method: " + payment.getPaymentMethod());
            System.out.println("Transaction ID: " + payment.getTransactionId());
            if (payment.getRefundAmount() > 0) {
                System.out.println("Refund Amount: $" + payment.getRefundAmount());
                System.out.println("Refund Date: " + payment.getRefundDate());
            }
        }
    }
}
