package com.lld.urlshortener.models;

import com.lld.urlshortener.enums.UrlStatus;
import java.time.LocalDateTime;

public class UrlMapping {
    private long id;
    private String shortCode;
    private String longUrl;
    private String longUrlHash;
    private LocalDateTime createdAt;
    private LocalDateTime expiryTime;
    private UrlStatus status;
    private long clickCount;
    private String createdBy;

    public UrlMapping(long id, String shortCode, String longUrl) {
        this.id = id;
        this.shortCode = shortCode;
        this.longUrl = longUrl;
        this.createdAt = LocalDateTime.now();
        this.status = UrlStatus.ACTIVE;
        this.clickCount = 0;
    }

    public boolean isExpired() {
        if (expiryTime == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiryTime);
    }

    public synchronized void incrementClickCount() {
        this.clickCount++;
    }

    public String getShortUrl(String baseUrl) {
        return baseUrl + "/" + shortCode;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }

    public String getLongUrlHash() { return longUrlHash; }
    public void setLongUrlHash(String longUrlHash) { this.longUrlHash = longUrlHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public UrlStatus getStatus() { return status; }
    public void setStatus(UrlStatus status) { this.status = status; }

    public long getClickCount() { return clickCount; }
    public void setClickCount(long clickCount) { this.clickCount = clickCount; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Override
    public String toString() {
        return "UrlMapping{" +
                "id=" + id +
                ", shortCode='" + shortCode + '\'' +
                ", longUrl='" + longUrl + '\'' +
                ", clickCount=" + clickCount +
                ", status=" + status +
                ", expiryTime=" + expiryTime +
                '}';
    }
}
