package com.lld.airbnb.models;

import com.lld.airbnb.enums.Amenity;
import com.lld.airbnb.enums.CancellationPolicy;
import com.lld.airbnb.enums.PropertyType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Property {
    private String propertyId;
    private String hostId;
    private String title;
    private String description;
    private PropertyType type;
    private Location location;
    private int maxGuests;
    private int bedrooms;
    private int bathrooms;
    private List<Amenity> amenities;
    private List<String> photoUrls;
    private double pricePerNight;
    private double cleaningFee;
    private double serviceFee;
    private CancellationPolicy cancellationPolicy;
    private PropertyCalendar calendar;
    private List<String> reviewIds;
    private double avgRating;
    private int totalReviews;
    private int totalBookings;
    private boolean instantBooking;
    private LocalDateTime createdAt;

    public Property(String propertyId, String hostId, String title, String description,
                   PropertyType type, Location location, int maxGuests, int bedrooms,
                   int bathrooms, double pricePerNight) {
        this.propertyId = propertyId;
        this.hostId = hostId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.location = location;
        this.maxGuests = maxGuests;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.pricePerNight = pricePerNight;
        this.amenities = new ArrayList<>();
        this.photoUrls = new ArrayList<>();
        this.reviewIds = new ArrayList<>();
        this.calendar = new PropertyCalendar(propertyId);
        this.cleaningFee = 50.0;  // Default
        this.serviceFee = 30.0;   // Default
        this.cancellationPolicy = CancellationPolicy.FLEXIBLE;
        this.avgRating = 0.0;
        this.totalReviews = 0;
        this.totalBookings = 0;
        this.instantBooking = true;  // Default to instant booking
        this.createdAt = LocalDateTime.now();
    }

    public void addAmenity(Amenity amenity) {
        if (!amenities.contains(amenity)) {
            amenities.add(amenity);
        }
    }

    public void addPhoto(String photoUrl) {
        photoUrls.add(photoUrl);
    }

    public void addReview(String reviewId) {
        reviewIds.add(reviewId);
    }

    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        return calendar.isAvailable(checkIn, checkOut);
    }

    public void blockDates(LocalDate checkIn, LocalDate checkOut) {
        calendar.blockDates(checkIn, checkOut);
    }

    public void unblockDates(LocalDate checkIn, LocalDate checkOut) {
        calendar.unblockDates(checkIn, checkOut);
    }

    public double calculateTotalCost(LocalDate checkIn, LocalDate checkOut) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        return (pricePerNight * nights) + cleaningFee + serviceFee;
    }

    public void updateRating(double newRating) {
        this.avgRating = ((avgRating * totalReviews) + newRating) / (totalReviews + 1);
        this.totalReviews++;
    }

    public void incrementBookingCount() {
        this.totalBookings++;
    }

    // Getters and Setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public PropertyType getType() { return type; }
    public void setType(PropertyType type) { this.type = type; }

    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }

    public int getMaxGuests() { return maxGuests; }
    public void setMaxGuests(int maxGuests) { this.maxGuests = maxGuests; }

    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public int getBathrooms() { return bathrooms; }
    public void setBathrooms(int bathrooms) { this.bathrooms = bathrooms; }

    public List<Amenity> getAmenities() { return amenities; }
    public void setAmenities(List<Amenity> amenities) { this.amenities = amenities; }

    public List<String> getPhotoUrls() { return photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls; }

    public double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(double pricePerNight) { this.pricePerNight = pricePerNight; }

    public double getCleaningFee() { return cleaningFee; }
    public void setCleaningFee(double cleaningFee) { this.cleaningFee = cleaningFee; }

    public double getServiceFee() { return serviceFee; }
    public void setServiceFee(double serviceFee) { this.serviceFee = serviceFee; }

    public CancellationPolicy getCancellationPolicy() { return cancellationPolicy; }
    public void setCancellationPolicy(CancellationPolicy cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public PropertyCalendar getCalendar() { return calendar; }
    public void setCalendar(PropertyCalendar calendar) { this.calendar = calendar; }

    public List<String> getReviewIds() { return reviewIds; }
    public void setReviewIds(List<String> reviewIds) { this.reviewIds = reviewIds; }

    public double getAvgRating() { return avgRating; }
    public void setAvgRating(double avgRating) { this.avgRating = avgRating; }

    public int getTotalReviews() { return totalReviews; }
    public void setTotalReviews(int totalReviews) { this.totalReviews = totalReviews; }

    public int getTotalBookings() { return totalBookings; }
    public void setTotalBookings(int totalBookings) { this.totalBookings = totalBookings; }

    public boolean isInstantBooking() { return instantBooking; }
    public void setInstantBooking(boolean instantBooking) { this.instantBooking = instantBooking; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Property{" +
                "id='" + propertyId + '\'' +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", location=" + location +
                ", price=$" + pricePerNight + "/night" +
                ", rating=" + String.format("%.1f", avgRating) +
                ", totalBookings=" + totalBookings +
                '}';
    }
}
