package com.lld.airbnb.services;

import com.lld.airbnb.enums.Amenity;
import com.lld.airbnb.enums.PropertyType;
import com.lld.airbnb.models.Location;
import com.lld.airbnb.models.Property;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class SearchService {
    private PropertyService propertyService;

    public SearchService(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // Search properties by location (within radius) and dates
    public List<Property> searchByLocation(Location centerLocation, double radiusKm,
                                          LocalDate checkIn, LocalDate checkOut,
                                          int guests) {
        List<Property> allProperties = propertyService.getAllProperties();
        List<Property> results = new ArrayList<>();

        for (Property property : allProperties) {
            // Check if property is within radius
            double distance = centerLocation.calculateDistance(property.getLocation());

            if (distance <= radiusKm) {
                // Check if available for the dates
                if (property.isAvailable(checkIn, checkOut)) {
                    // Check if property can accommodate guests
                    if (property.getMaxGuests() >= guests) {
                        results.add(property);
                    }
                }
            }
        }

        System.out.println("Found " + results.size() + " properties within " + radiusKm +
                          "km of " + centerLocation.getCity());
        return results;
    }

    // Search properties by city name
    public List<Property> searchByCity(String city, LocalDate checkIn, LocalDate checkOut, int guests) {
        List<Property> allProperties = propertyService.getAllProperties();
        List<Property> results = new ArrayList<>();

        for (Property property : allProperties) {
            if (property.getLocation().getCity().equalsIgnoreCase(city)) {
                if (property.isAvailable(checkIn, checkOut)) {
                    if (property.getMaxGuests() >= guests) {
                        results.add(property);
                    }
                }
            }
        }

        System.out.println("Found " + results.size() + " properties in " + city);
        return results;
    }

    // Filter by price range
    public List<Property> filterByPriceRange(List<Property> properties, double minPrice, double maxPrice) {
        return properties.stream()
                .filter(p -> p.getPricePerNight() >= minPrice && p.getPricePerNight() <= maxPrice)
                .collect(Collectors.toList());
    }

    // Filter by property type
    public List<Property> filterByPropertyType(List<Property> properties, PropertyType type) {
        return properties.stream()
                .filter(p -> p.getType() == type)
                .collect(Collectors.toList());
    }

    // Filter by amenities (property must have ALL specified amenities)
    public List<Property> filterByAmenities(List<Property> properties, List<Amenity> requiredAmenities) {
        return properties.stream()
                .filter(p -> p.getAmenities().containsAll(requiredAmenities))
                .collect(Collectors.toList());
    }

    // Filter by minimum bedrooms
    public List<Property> filterByBedrooms(List<Property> properties, int minBedrooms) {
        return properties.stream()
                .filter(p -> p.getBedrooms() >= minBedrooms)
                .collect(Collectors.toList());
    }

    // Sort by price (ascending or descending)
    public List<Property> sortByPrice(List<Property> properties, boolean ascending) {
        List<Property> sorted = new ArrayList<>(properties);
        sorted.sort(Comparator.comparingDouble(Property::getPricePerNight));
        if (!ascending) {
            Collections.reverse(sorted);
        }
        return sorted;
    }

    // Sort by rating (highest first)
    public List<Property> sortByRating(List<Property> properties) {
        List<Property> sorted = new ArrayList<>(properties);
        sorted.sort((p1, p2) -> Double.compare(p2.getAvgRating(), p1.getAvgRating()));
        return sorted;
    }

    // Sort by popularity (most bookings first)
    public List<Property> sortByPopularity(List<Property> properties) {
        List<Property> sorted = new ArrayList<>(properties);
        sorted.sort((p1, p2) -> Integer.compare(p2.getTotalBookings(), p1.getTotalBookings()));
        return sorted;
    }

    public void displaySearchResults(List<Property> properties) {
        System.out.println("\n=== Search Results (" + properties.size() + " properties) ===");
        for (int i = 0; i < properties.size(); i++) {
            Property p = properties.get(i);
            System.out.println((i + 1) + ". " + p.getTitle() +
                             " | " + p.getType() +
                             " | " + p.getLocation().getCity() +
                             " | $" + p.getPricePerNight() + "/night" +
                             " | ★" + String.format("%.1f", p.getAvgRating()) +
                             " | " + p.getBedrooms() + "BR/" + p.getBathrooms() + "BA" +
                             " | Max " + p.getMaxGuests() + " guests");
        }
    }
}
