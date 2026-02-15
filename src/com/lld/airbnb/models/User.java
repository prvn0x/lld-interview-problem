package com.lld.airbnb.models;

import com.lld.airbnb.enums.UserType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private UserType userType;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private List<String> hostedPropertyIds;  // Property IDs hosted by this user
    private List<String> bookingIds;         // Booking IDs made by this user
    private double avgRatingAsHost;
    private double avgRatingAsGuest;
    private int totalReviewsAsHost;
    private int totalReviewsAsGuest;

    public User(String userId, String name, String email, String phone, UserType userType) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.userType = userType;
        this.isVerified = false;
        this.createdAt = LocalDateTime.now();
        this.hostedPropertyIds = new ArrayList<>();
        this.bookingIds = new ArrayList<>();
        this.avgRatingAsHost = 0.0;
        this.avgRatingAsGuest = 0.0;
        this.totalReviewsAsHost = 0;
        this.totalReviewsAsGuest = 0;
    }

    public void addHostedProperty(String propertyId) {
        this.hostedPropertyIds.add(propertyId);
    }

    public void addBooking(String bookingId) {
        this.bookingIds.add(bookingId);
    }

    public void updateRatingAsHost(double newRating) {
        this.avgRatingAsHost = ((avgRatingAsHost * totalReviewsAsHost) + newRating) / (totalReviewsAsHost + 1);
        this.totalReviewsAsHost++;
    }

    public void updateRatingAsGuest(double newRating) {
        this.avgRatingAsGuest = ((avgRatingAsGuest * totalReviewsAsGuest) + newRating) / (totalReviewsAsGuest + 1);
        this.totalReviewsAsGuest++;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public UserType getUserType() { return userType; }
    public void setUserType(UserType userType) { this.userType = userType; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getHostedPropertyIds() { return hostedPropertyIds; }
    public void setHostedPropertyIds(List<String> hostedPropertyIds) { this.hostedPropertyIds = hostedPropertyIds; }

    public List<String> getBookingIds() { return bookingIds; }
    public void setBookingIds(List<String> bookingIds) { this.bookingIds = bookingIds; }

    public double getAvgRatingAsHost() { return avgRatingAsHost; }
    public void setAvgRatingAsHost(double avgRatingAsHost) { this.avgRatingAsHost = avgRatingAsHost; }

    public double getAvgRatingAsGuest() { return avgRatingAsGuest; }
    public void setAvgRatingAsGuest(double avgRatingAsGuest) { this.avgRatingAsGuest = avgRatingAsGuest; }

    public int getTotalReviewsAsHost() { return totalReviewsAsHost; }
    public void setTotalReviewsAsHost(int totalReviewsAsHost) { this.totalReviewsAsHost = totalReviewsAsHost; }

    public int getTotalReviewsAsGuest() { return totalReviewsAsGuest; }
    public void setTotalReviewsAsGuest(int totalReviewsAsGuest) { this.totalReviewsAsGuest = totalReviewsAsGuest; }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", userType=" + userType +
                ", avgRatingAsHost=" + String.format("%.1f", avgRatingAsHost) +
                '}';
    }
}
