package com.lld.airbnb.enums;

public enum CancellationPolicy {
    FLEXIBLE,       // Full refund if cancelled 24 hours before
    MODERATE,       // Full refund if cancelled 5 days before
    STRICT          // 50% refund if cancelled 7 days before
}
