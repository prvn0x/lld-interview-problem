package com.lld.airbnb.services;

import com.lld.airbnb.enums.UserType;
import com.lld.airbnb.models.User;
import java.util.*;

public class UserService {
    private Map<String, User> users;
    private Map<String, User> usersByEmail;

    public UserService() {
        this.users = new HashMap<>();
        this.usersByEmail = new HashMap<>();
    }

    public User registerUser(String userId, String name, String email, String phone, UserType userType) {
        if (usersByEmail.containsKey(email)) {
            System.out.println("User with email " + email + " already exists!");
            return usersByEmail.get(email);
        }

        User user = new User(userId, name, email, phone, userType);
        users.put(userId, user);
        usersByEmail.put(email, user);

        System.out.println("User registered: " + user);
        return user;
    }

    public User getUserById(String userId) {
        return users.get(userId);
    }

    public User getUserByEmail(String email) {
        return usersByEmail.get(email);
    }

    public void verifyUser(String userId) {
        User user = users.get(userId);
        if (user != null) {
            user.setVerified(true);
            System.out.println("User " + userId + " verified successfully");
        }
    }

    public List<String> getHostedProperties(String userId) {
        User user = users.get(userId);
        return user != null ? user.getHostedPropertyIds() : new ArrayList<>();
    }

    public List<String> getUserBookings(String userId) {
        User user = users.get(userId);
        return user != null ? user.getBookingIds() : new ArrayList<>();
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public void displayUser(String userId) {
        User user = users.get(userId);
        if (user != null) {
            System.out.println("\n=== User Details ===");
            System.out.println("ID: " + user.getUserId());
            System.out.println("Name: " + user.getName());
            System.out.println("Email: " + user.getEmail());
            System.out.println("Type: " + user.getUserType());
            System.out.println("Verified: " + user.isVerified());
            System.out.println("Properties hosted: " + user.getHostedPropertyIds().size());
            System.out.println("Bookings made: " + user.getBookingIds().size());
            System.out.println("Rating as Host: " + String.format("%.1f", user.getAvgRatingAsHost()));
            System.out.println("Rating as Guest: " + String.format("%.1f", user.getAvgRatingAsGuest()));
        }
    }
}
