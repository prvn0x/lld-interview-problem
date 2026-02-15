package com.lld.urlshortener.models;

import java.time.LocalDateTime;

public class AnalyticsEvent {
    private String shortCode;
    private LocalDateTime timestamp;
    private String userAgent;
    private String ipAddress;

    public AnalyticsEvent(String shortCode) {
        this.shortCode = shortCode;
        this.timestamp = LocalDateTime.now();
    }

    public AnalyticsEvent(String shortCode, String userAgent, String ipAddress) {
        this(shortCode);
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    @Override
    public String toString() {
        return "AnalyticsEvent{" +
                "shortCode='" + shortCode + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
