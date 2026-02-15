package com.lld.airbnb.models;

import com.lld.airbnb.enums.PaymentStatus;
import java.time.LocalDateTime;

public class Payment {
    private String paymentId;
    private String bookingId;
    private double amount;
    private PaymentStatus status;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String transactionId;
    private double refundAmount;
    private LocalDateTime refundDate;

    public Payment(String paymentId, String bookingId, double amount, String paymentMethod) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.PENDING;
        this.refundAmount = 0.0;
    }

    public boolean processPayment() {
        // Simulate payment processing
        this.status = PaymentStatus.SUCCESS;
        this.paymentDate = LocalDateTime.now();
        this.transactionId = "TXN" + System.currentTimeMillis();
        return true;
    }

    public boolean processRefund(double refundAmt) {
        if (this.status != PaymentStatus.SUCCESS) {
            return false;
        }

        if (refundAmt > this.amount) {
            refundAmt = this.amount;
        }

        this.refundAmount = refundAmt;
        this.refundDate = LocalDateTime.now();
        this.status = PaymentStatus.REFUNDED;
        return true;
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double refundAmount) { this.refundAmount = refundAmount; }

    public LocalDateTime getRefundDate() { return refundDate; }
    public void setRefundDate(LocalDateTime refundDate) { this.refundDate = refundDate; }

    @Override
    public String toString() {
        return "Payment{" +
                "id='" + paymentId + '\'' +
                ", bookingId='" + bookingId + '\'' +
                ", amount=$" + amount +
                ", status=" + status +
                ", method='" + paymentMethod + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}
