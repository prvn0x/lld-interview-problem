package com.lld.airbnb.enums;

public enum BookingStatus {
    PENDING,        // Waiting for host approval
    CONFIRMED,      // Approved by host or instant booked
    CANCELLED,      // Cancelled by guest or host
    COMPLETED       // After checkout
}
