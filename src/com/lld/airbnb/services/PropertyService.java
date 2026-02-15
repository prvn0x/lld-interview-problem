package com.lld.airbnb.services;

import com.lld.airbnb.enums.Amenity;
import com.lld.airbnb.enums.PropertyType;
import com.lld.airbnb.models.Location;
import com.lld.airbnb.models.Property;
import com.lld.airbnb.models.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class PropertyService {
    private Map<String, Property> properties;
    private UserService userService;

    public PropertyService(UserService userService) {
        this.properties = new HashMap<>();
        this.userService = userService;
    }

    public Property createProperty(String propertyId, String hostId, String title,
                                  String description, PropertyType type, Location location,
                                  int maxGuests, int bedrooms, int bathrooms, double pricePerNight) {
        User host = userService.getUserById(hostId);
        if (host == null) {
            System.out.println("Host not found: " + hostId);
            return null;
        }

        Property property = new Property(propertyId, hostId, title, description, type,
                                        location, maxGuests, bedrooms, bathrooms, pricePerNight);
        properties.put(propertyId, property);
        host.addHostedProperty(propertyId);

        System.out.println("Property created: " + property);
        return property;
    }

    public Property getPropertyById(String propertyId) {
        return properties.get(propertyId);
    }

    public void updateProperty(String propertyId, String title, String description, double pricePerNight) {
        Property property = properties.get(propertyId);
        if (property != null) {
            property.setTitle(title);
            property.setDescription(description);
            property.setPricePerNight(pricePerNight);
            System.out.println("Property updated: " + propertyId);
        }
    }

    public void deleteProperty(String propertyId) {
        Property property = properties.remove(propertyId);
        if (property != null) {
            User host = userService.getUserById(property.getHostId());
            if (host != null) {
                host.getHostedPropertyIds().remove(propertyId);
            }
            System.out.println("Property deleted: " + propertyId);
        }
    }

    public void addAmenity(String propertyId, Amenity amenity) {
        Property property = properties.get(propertyId);
        if (property != null) {
            property.addAmenity(amenity);
        }
    }

    public void addPhoto(String propertyId, String photoUrl) {
        Property property = properties.get(propertyId);
        if (property != null) {
            property.addPhoto(photoUrl);
        }
    }

    public List<Property> getPropertiesByHost(String hostId) {
        return properties.values().stream()
                .filter(p -> p.getHostId().equals(hostId))
                .collect(Collectors.toList());
    }

    public List<Property> getAllProperties() {
        return new ArrayList<>(properties.values());
    }

    public boolean isAvailable(String propertyId, LocalDate checkIn, LocalDate checkOut) {
        Property property = properties.get(propertyId);
        return property != null && property.isAvailable(checkIn, checkOut);
    }

    public void displayProperty(String propertyId) {
        Property property = properties.get(propertyId);
        if (property != null) {
            System.out.println("\n=== Property Details ===");
            System.out.println("ID: " + property.getPropertyId());
            System.out.println("Title: " + property.getTitle());
            System.out.println("Description: " + property.getDescription());
            System.out.println("Type: " + property.getType());
            System.out.println("Location: " + property.getLocation());
            System.out.println("Max Guests: " + property.getMaxGuests());
            System.out.println("Bedrooms: " + property.getBedrooms());
            System.out.println("Bathrooms: " + property.getBathrooms());
            System.out.println("Price: $" + property.getPricePerNight() + "/night");
            System.out.println("Cleaning Fee: $" + property.getCleaningFee());
            System.out.println("Service Fee: $" + property.getServiceFee());
            System.out.println("Amenities: " + property.getAmenities());
            System.out.println("Rating: " + String.format("%.1f", property.getAvgRating()) +
                             " (" + property.getTotalReviews() + " reviews)");
            System.out.println("Total Bookings: " + property.getTotalBookings());
            System.out.println("Instant Booking: " + property.isInstantBooking());
        }
    }
}
