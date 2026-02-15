package com.lld.airbnb;

import com.lld.airbnb.enums.*;
import com.lld.airbnb.models.*;
import com.lld.airbnb.services.*;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════");
        System.out.println("     AIRBNB - LOW LEVEL DESIGN DEMO");
        System.out.println("═══════════════════════════════════════════\n");

        // Initialize all services
        UserService userService = new UserService();
        PropertyService propertyService = new PropertyService(userService);
        PaymentService paymentService = new PaymentService(propertyService);
        BookingService bookingService = new BookingService(propertyService, userService, paymentService);
        SearchService searchService = new SearchService(propertyService);
        ReviewService reviewService = new ReviewService(propertyService, userService);
        MessagingService messagingService = new MessagingService();

        // ===== STEP 1: Register Users =====
        System.out.println("\n▶ STEP 1: Register Users");
        System.out.println("─────────────────────────────────────────\n");

        User host1 = userService.registerUser("H001", "John Host", "john@host.com",
                                             "+1-555-0101", UserType.HOST);
        User host2 = userService.registerUser("H002", "Sarah Landlord", "sarah@host.com",
                                             "+1-555-0102", UserType.HOST);
        User guest1 = userService.registerUser("G001", "Alice Guest", "alice@guest.com",
                                              "+1-555-0201", UserType.GUEST);
        User guest2 = userService.registerUser("G002", "Bob Traveler", "bob@guest.com",
                                              "+1-555-0202", UserType.GUEST);

        userService.verifyUser("H001");
        userService.verifyUser("G001");

        // ===== STEP 2: Create Properties =====
        System.out.println("\n▶ STEP 2: Create Properties with Amenities");
        System.out.println("─────────────────────────────────────────\n");

        // Property 1: NYC Apartment
        Location nycLocation = new Location("123 Broadway", "New York", "NY", "USA",
                                           "10001", 40.7128, -74.0060);
        Property prop1 = propertyService.createProperty("P001", "H001",
                                    "Cozy Manhattan Apartment",
                                    "Beautiful 2BR apartment in the heart of Manhattan with city views",
                                    PropertyType.APARTMENT, nycLocation, 4, 2, 2, 150.0);
        prop1.addAmenity(Amenity.WIFI);
        prop1.addAmenity(Amenity.KITCHEN);
        prop1.addAmenity(Amenity.AIR_CONDITIONING);
        prop1.addAmenity(Amenity.TV);
        prop1.addAmenity(Amenity.ELEVATOR);
        prop1.addPhoto("https://example.com/photos/nyc-apt-1.jpg");
        prop1.setCancellationPolicy(CancellationPolicy.FLEXIBLE);

        // Property 2: San Francisco House
        Location sfLocation = new Location("456 Market St", "San Francisco", "CA", "USA",
                                          "94102", 37.7749, -122.4194);
        Property prop2 = propertyService.createProperty("P002", "H001",
                                    "Modern SF House",
                                    "Spacious 3BR house in San Francisco with parking",
                                    PropertyType.HOUSE, sfLocation, 6, 3, 2, 200.0);
        prop2.addAmenity(Amenity.WIFI);
        prop2.addAmenity(Amenity.KITCHEN);
        prop2.addAmenity(Amenity.PARKING);
        prop2.addAmenity(Amenity.WASHER);
        prop2.addAmenity(Amenity.DRYER);
        prop2.addAmenity(Amenity.WORKSPACE);
        prop2.addPhoto("https://example.com/photos/sf-house-1.jpg");
        prop2.setCancellationPolicy(CancellationPolicy.MODERATE);

        // Property 3: NYC Studio (near property 1)
        Location nycLocation2 = new Location("789 5th Ave", "New York", "NY", "USA",
                                            "10002", 40.7580, -73.9855);
        Property prop3 = propertyService.createProperty("P003", "H002",
                                    "Luxury NYC Studio",
                                    "Modern studio with amazing views of Central Park",
                                    PropertyType.STUDIO, nycLocation2, 2, 1, 1, 120.0);
        prop3.addAmenity(Amenity.WIFI);
        prop3.addAmenity(Amenity.GYM);
        prop3.addAmenity(Amenity.POOL);
        prop3.addAmenity(Amenity.SECURITY);
        prop3.setCancellationPolicy(CancellationPolicy.STRICT);
        prop3.setInstantBooking(false);  // Requires host approval

        // Property 4: LA Villa
        Location laLocation = new Location("321 Sunset Blvd", "Los Angeles", "CA", "USA",
                                          "90028", 34.0522, -118.2437);
        Property prop4 = propertyService.createProperty("P004", "H002",
                                    "Beverly Hills Villa",
                                    "Luxury 4BR villa with pool and stunning views",
                                    PropertyType.VILLA, laLocation, 8, 4, 3, 350.0);
        prop4.addAmenity(Amenity.WIFI);
        prop4.addAmenity(Amenity.POOL);
        prop4.addAmenity(Amenity.HOT_TUB);
        prop4.addAmenity(Amenity.PARKING);
        prop4.addAmenity(Amenity.GYM);
        prop4.setCancellationPolicy(CancellationPolicy.FLEXIBLE);

        // ===== STEP 3: Search Properties =====
        System.out.println("\n▶ STEP 3: Search Properties");
        System.out.println("─────────────────────────────────────────\n");

        LocalDate checkIn = LocalDate.now().plusDays(30);
        LocalDate checkOut = LocalDate.now().plusDays(35);
        int guests = 2;

        System.out.println("📍 Searching in New York for " + guests + " guests");
        System.out.println("   Check-in: " + checkIn + " | Check-out: " + checkOut);
        List<Property> nycResults = searchService.searchByCity("New York", checkIn, checkOut, guests);
        searchService.displaySearchResults(nycResults);

        // Search by radius
        System.out.println("\n📍 Searching within 10km of Manhattan center");
        List<Property> nearbyResults = searchService.searchByLocation(nycLocation, 10.0,
                                                                      checkIn, checkOut, guests);
        searchService.displaySearchResults(nearbyResults);

        // ===== STEP 4: Filter and Sort =====
        System.out.println("\n▶ STEP 4: Apply Filters and Sorting");
        System.out.println("─────────────────────────────────────────\n");

        System.out.println("💰 Filter: Properties under $160/night");
        List<Property> affordableProperties = searchService.filterByPriceRange(nycResults, 0, 160);
        searchService.displaySearchResults(affordableProperties);

        System.out.println("\n🔝 Sort: By price (ascending)");
        List<Property> sortedByPrice = searchService.sortByPrice(nycResults, true);
        searchService.displaySearchResults(sortedByPrice);

        // ===== STEP 5: View Property Details =====
        System.out.println("\n▶ STEP 5: View Property Details");
        System.out.println("─────────────────────────────────────────");
        propertyService.displayProperty("P001");

        // Calculate cost
        double totalCost = prop1.calculateTotalCost(checkIn, checkOut);
        System.out.println("\n💵 Total Cost Calculation:");
        System.out.println("   5 nights × $" + prop1.getPricePerNight() + " = $" +
                          (5 * prop1.getPricePerNight()));
        System.out.println("   Cleaning fee: $" + prop1.getCleaningFee());
        System.out.println("   Service fee: $" + prop1.getServiceFee());
        System.out.println("   TOTAL: $" + totalCost);

        // ===== STEP 6: Create Booking =====
        System.out.println("\n▶ STEP 6: Create Booking");
        System.out.println("─────────────────────────────────────────\n");

        Booking booking1 = bookingService.createBooking("B001", "P001", "G001",
                                                       checkIn, checkOut, 2);

        // ===== STEP 7: Process Payment =====
        System.out.println("\n▶ STEP 7: Process Payment");
        System.out.println("─────────────────────────────────────────\n");

        if (booking1 != null) {
            Payment payment1 = paymentService.processPayment("PAY001", "B001",
                                                            booking1.getTotalAmount(), "CARD");
            booking1.setPaymentId("PAY001");
            paymentService.displayPayment("PAY001");
        }

        // ===== STEP 8: Messaging between Host and Guest =====
        System.out.println("\n▶ STEP 8: Host-Guest Communication");
        System.out.println("─────────────────────────────────────────\n");

        messagingService.sendMessage("M001", "G001", "H001",
                                    "Hi! I'm arriving around 3 PM. Is early check-in possible?", "B001");
        messagingService.sendMessage("M002", "H001", "G001",
                                    "Hello! Yes, early check-in is fine. See you then!", "B001");
        messagingService.sendMessage("M003", "G001", "H001",
                                    "Great! Thank you so much!", "B001");

        messagingService.displayConversation("G001", "H001");

        // ===== STEP 9: Complete Booking and Add Reviews =====
        System.out.println("\n▶ STEP 9: Complete Booking and Add Reviews");
        System.out.println("─────────────────────────────────────────\n");

        bookingService.completeBooking("B001");

        // Guest reviews property
        reviewService.addPropertyReview("R001", "B001", "G001", "P001", 5,
                                       "Amazing apartment! Clean, comfortable, and great location. " +
                                       "John was a wonderful host. Highly recommend!");

        // Host reviews guest
        reviewService.addGuestReview("R002", "B001", "H001", "G001", 5,
                                    "Alice was a great guest! Very respectful and communicative. " +
                                    "Would love to host her again!");

        // Display property reviews
        reviewService.displayPropertyReviews("P001");

        // ===== STEP 10: Create Another Booking (with manual approval) =====
        System.out.println("\n▶ STEP 10: Booking Requiring Host Approval");
        System.out.println("─────────────────────────────────────────\n");

        LocalDate checkIn2 = LocalDate.now().plusDays(45);
        LocalDate checkOut2 = LocalDate.now().plusDays(48);

        Booking booking2 = bookingService.createBooking("B002", "P003", "G002",
                                                       checkIn2, checkOut2, 2);

        System.out.println("\n⏳ Host approves the booking...");
        bookingService.confirmBooking("B002");

        // ===== STEP 11: Cancellation and Refund =====
        System.out.println("\n▶ STEP 11: Booking Cancellation with Refund");
        System.out.println("─────────────────────────────────────────\n");

        // Create a booking that will be cancelled
        LocalDate checkIn3 = LocalDate.now().plusDays(10);
        LocalDate checkOut3 = LocalDate.now().plusDays(15);

        Booking booking3 = bookingService.createBooking("B003", "P002", "G001",
                                                       checkIn3, checkOut3, 4);
        if (booking3 != null) {
            Payment payment3 = paymentService.processPayment("PAY003", "B003",
                                                            booking3.getTotalAmount(), "UPI");
            booking3.setPaymentId("PAY003");

            System.out.println("\n❌ Guest cancels the booking...");
            bookingService.cancelBooking("B003", "Change of plans");

            paymentService.displayPayment("PAY003");
        }

        // ===== STEP 12: Display User Stats =====
        System.out.println("\n▶ STEP 12: Display User Statistics");
        System.out.println("─────────────────────────────────────────");
        userService.displayUser("H001");
        userService.displayUser("G001");

        // ===== STEP 13: Host's Dashboard =====
        System.out.println("\n▶ STEP 13: Host Dashboard");
        System.out.println("─────────────────────────────────────────\n");

        List<Property> host1Properties = propertyService.getPropertiesByHost("H001");
        System.out.println("John's Properties: " + host1Properties.size());
        for (Property p : host1Properties) {
            System.out.println("  - " + p);
        }

        List<Booking> host1Bookings = bookingService.getBookingsByHost("H001");
        System.out.println("\nJohn's Bookings: " + host1Bookings.size());
        for (Booking b : host1Bookings) {
            System.out.println("  - " + b);
        }

        // ===== STEP 14: Guest's Dashboard =====
        System.out.println("\n▶ STEP 14: Guest Dashboard");
        System.out.println("─────────────────────────────────────────\n");

        List<Booking> guest1Bookings = bookingService.getBookingsByGuest("G001");
        System.out.println("Alice's Bookings: " + guest1Bookings.size());
        for (Booking b : guest1Bookings) {
            System.out.println("  - " + b);
        }

        // ===== Final Summary =====
        System.out.println("\n═══════════════════════════════════════════");
        System.out.println("         DEMO COMPLETED SUCCESSFULLY!");
        System.out.println("═══════════════════════════════════════════");
        System.out.println("\n📊 Summary:");
        System.out.println("   ✓ Users registered: " + userService.getAllUsers().size());
        System.out.println("   ✓ Properties listed: " + propertyService.getAllProperties().size());
        System.out.println("   ✓ Bookings made: " + bookingService.getAllBookings().size());
        System.out.println("   ✓ Reviews added: 2");
        System.out.println("   ✓ Messages exchanged: 3");
        System.out.println("\n✨ Key Features Demonstrated:");
        System.out.println("   • User registration (Host/Guest)");
        System.out.println("   • Property listing with amenities");
        System.out.println("   • Geolocation-based search");
        System.out.println("   • Filtering and sorting");
        System.out.println("   • Booking with availability check");
        System.out.println("   • Payment processing");
        System.out.println("   • Host-guest messaging");
        System.out.println("   • Review and rating system");
        System.out.println("   • Cancellation with refund (based on policy)");
        System.out.println("   • Instant booking vs. manual approval");
        System.out.println("\n═══════════════════════════════════════════\n");
    }
}
