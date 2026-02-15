package com.lld.airbnb.services;

import com.lld.airbnb.models.Property;
import com.lld.airbnb.models.Review;
import com.lld.airbnb.models.User;
import java.util.*;
import java.util.stream.Collectors;

public class ReviewService {
    private Map<String, Review> reviews;
    private PropertyService propertyService;
    private UserService userService;

    public ReviewService(PropertyService propertyService, UserService userService) {
        this.reviews = new HashMap<>();
        this.propertyService = propertyService;
        this.userService = userService;
    }

    // Guest reviews property after stay
    public Review addPropertyReview(String reviewId, String bookingId, String guestId,
                                   String propertyId, int rating, String comment) {
        Property property = propertyService.getPropertyById(propertyId);
        if (property == null) {
            System.out.println("Property not found: " + propertyId);
            return null;
        }

        Review review = new Review(reviewId, bookingId, guestId, property.getHostId(),
                                  propertyId, rating, comment);
        reviews.put(reviewId, review);
        property.addReview(reviewId);
        property.updateRating(rating);

        // Update host rating
        User host = userService.getUserById(property.getHostId());
        if (host != null) {
            host.updateRatingAsHost(rating);
        }

        System.out.println("Review added for property: " + propertyId);
        System.out.println(review);
        return review;
    }

    // Host reviews guest after stay
    public Review addGuestReview(String reviewId, String bookingId, String hostId,
                                String guestId, int rating, String comment) {
        User guest = userService.getUserById(guestId);
        if (guest == null) {
            System.out.println("Guest not found: " + guestId);
            return null;
        }

        Review review = new Review(reviewId, bookingId, hostId, guestId,
                                  null, rating, comment);
        reviews.put(reviewId, review);
        guest.updateRatingAsGuest(rating);

        System.out.println("Review added for guest: " + guestId);
        System.out.println(review);
        return review;
    }

    public Review getReviewById(String reviewId) {
        return reviews.get(reviewId);
    }

    public List<Review> getReviewsForProperty(String propertyId) {
        return reviews.values().stream()
                .filter(r -> propertyId.equals(r.getPropertyId()))
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsByReviewer(String reviewerId) {
        return reviews.values().stream()
                .filter(r -> reviewerId.equals(r.getReviewerId()))
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsForUser(String userId) {
        return reviews.values().stream()
                .filter(r -> userId.equals(r.getRevieweeId()))
                .collect(Collectors.toList());
    }

    public double calculateAverageRating(List<Review> reviewList) {
        if (reviewList.isEmpty()) {
            return 0.0;
        }
        double sum = reviewList.stream().mapToInt(Review::getRating).sum();
        return sum / reviewList.size();
    }

    public void displayPropertyReviews(String propertyId) {
        List<Review> propertyReviews = getReviewsForProperty(propertyId);
        System.out.println("\n=== Reviews for Property " + propertyId + " ===");
        System.out.println("Total Reviews: " + propertyReviews.size());
        System.out.println("Average Rating: " + String.format("%.1f",
                          calculateAverageRating(propertyReviews)));
        System.out.println("\nReviews:");
        for (Review review : propertyReviews) {
            System.out.println("- " + review);
        }
    }
}
