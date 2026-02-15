package com.lld.urlshortener.models;

import java.time.LocalDateTime;

public class User {
    private String userId;
    private String apiKey;
    private String name;
    private LocalDateTime createdAt;
    private int dailyUrlLimit;
    private int urlsCreatedToday;

    public User(String userId, String apiKey, String name) {
        this.userId = userId;
        this.apiKey = apiKey;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.dailyUrlLimit = 1000;  // Default limit
        this.urlsCreatedToday = 0;
    }

    public boolean canCreateUrl() {
        return urlsCreatedToday < dailyUrlLimit;
    }

    public synchronized void incrementUrlCount() {
        this.urlsCreatedToday++;
    }

    public void resetDailyCount() {
        this.urlsCreatedToday = 0;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getDailyUrlLimit() { return dailyUrlLimit; }
    public void setDailyUrlLimit(int dailyUrlLimit) { this.dailyUrlLimit = dailyUrlLimit; }

    public int getUrlsCreatedToday() { return urlsCreatedToday; }
    public void setUrlsCreatedToday(int urlsCreatedToday) { this.urlsCreatedToday = urlsCreatedToday; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", urlsCreatedToday=" + urlsCreatedToday +
                ", dailyLimit=" + dailyUrlLimit +
                '}';
    }
}
