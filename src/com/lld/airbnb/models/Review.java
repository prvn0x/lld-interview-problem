package com.lld.airbnb.models;

import java.time.LocalDateTime;

public class Review {
    private String reviewId;
    private String bookingId;
    private String reviewerId;      // User who writes the review
    private String revieweeId;      // User/Property being reviewed
    private String propertyId;      // If reviewing property
    private int rating;             // 1-5 stars
    private String comment;
    private LocalDateTime reviewDate;

    public Review(String reviewId, String bookingId, String reviewerId,
                 String revieweeId, String propertyId, int rating, String comment) {
        this.reviewId = reviewId;
        this.bookingId = bookingId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.propertyId = propertyId;
        this.rating = Math.max(1, Math.min(5, rating));  // Clamp between 1-5
        this.comment = comment;
        this.reviewDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getReviewerId() { return reviewerId; }
    public void setReviewerId(String reviewerId) { this.reviewerId = reviewerId; }

    public String getRevieweeId() { return revieweeId; }
    public void setRevieweeId(String revieweeId) { this.revieweeId = revieweeId; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDateTime reviewDate) { this.reviewDate = reviewDate; }

    @Override
    public String toString() {
        return "Review{" +
                "id='" + reviewId + '\'' +
                ", rating=" + rating + " stars" +
                ", comment='" + comment + '\'' +
                ", date=" + reviewDate.toLocalDate() +
                '}';
    }
}
