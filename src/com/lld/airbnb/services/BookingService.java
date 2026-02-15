package com.lld.airbnb.services;

import com.lld.airbnb.enums.BookingStatus;
import com.lld.airbnb.models.Booking;
import com.lld.airbnb.models.Property;
import com.lld.airbnb.models.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BookingService {
    private Map<String, Booking> bookings;
    private PropertyService propertyService;
    private UserService userService;
    private PaymentService paymentService;

    public BookingService(PropertyService propertyService, UserService userService,
                         PaymentService paymentService) {
        this.bookings = new HashMap<>();
        this.propertyService = propertyService;
        this.userService = userService;
        this.paymentService = paymentService;
    }

    public Booking createBooking(String bookingId, String propertyId, String guestId,
                                LocalDate checkIn, LocalDate checkOut, int guests) {
        // Validate property exists
        Property property = propertyService.getPropertyById(propertyId);
        if (property == null) {
            System.out.println("Property not found: " + propertyId);
            return null;
        }

        // Validate guest exists
        User guest = userService.getUserById(guestId);
        if (guest == null) {
            System.out.println("Guest not found: " + guestId);
            return null;
        }

        // Check availability
        if (!property.isAvailable(checkIn, checkOut)) {
            System.out.println("Property not available for selected dates!");
            return null;
        }

        // Check guest capacity
        if (property.getMaxGuests() < guests) {
            System.out.println("Property can accommodate max " + property.getMaxGuests() + " guests!");
            return null;
        }

        // Calculate total cost
        double totalCost = property.calculateTotalCost(checkIn, checkOut);

        // Create booking
        Booking booking = new Booking(bookingId, propertyId, guestId, property.getHostId(),
                                     checkIn, checkOut, guests, totalCost);

        bookings.put(bookingId, booking);
        guest.addBooking(bookingId);

        // Block dates in property calendar
        property.blockDates(checkIn, checkOut);

        // Auto-confirm if instant booking enabled
        if (property.isInstantBooking()) {
            booking.confirm();
            property.incrementBookingCount();
            System.out.println("Booking auto-confirmed (Instant Booking): " + bookingId);
        } else {
            System.out.println("Booking created (pending host approval): " + bookingId);
        }

        System.out.println(booking);
        return booking;
    }

    public void confirmBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("Booking not found: " + bookingId);
            return;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            System.out.println("Booking is not in pending status!");
            return;
        }

        booking.confirm();
        Property property = propertyService.getPropertyById(booking.getPropertyId());
        if (property != null) {
            property.incrementBookingCount();
        }

        System.out.println("Booking confirmed: " + bookingId);
    }

    public void cancelBooking(String bookingId, String reason) {
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            System.out.println("Booking not found: " + bookingId);
            return;
        }

        if (booking.getStatus() == BookingStatus.CANCELLED ||
            booking.getStatus() == BookingStatus.COMPLETED) {
            System.out.println("Cannot cancel booking in " + booking.getStatus() + " status!");
            return;
        }

        // Unblock dates
        Property property = propertyService.getPropertyById(booking.getPropertyId());
        if (property != null) {
            property.unblockDates(booking.getCheckInDate(), booking.getCheckOutDate());
        }

        // Process refund based on cancellation policy
        if (booking.getPaymentId() != null) {
            paymentService.processRefundForBooking(booking);
        }

        booking.cancel(reason);
        System.out.println("Booking cancelled: " + bookingId + " | Reason: " + reason);
    }

    public void completeBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null && booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.complete();
            System.out.println("Booking completed: " + bookingId);
        }
    }

    public Booking getBookingById(String bookingId) {
        return bookings.get(bookingId);
    }

    public List<Booking> getBookingsByGuest(String guestId) {
        return bookings.values().stream()
                .filter(b -> b.getGuestId().equals(guestId))
                .collect(Collectors.toList());
    }

    public List<Booking> getBookingsByHost(String hostId) {
        return bookings.values().stream()
                .filter(b -> b.getHostId().equals(hostId))
                .collect(Collectors.toList());
    }

    public List<Booking> getBookingsByProperty(String propertyId) {
        return bookings.values().stream()
                .filter(b -> b.getPropertyId().equals(propertyId))
                .collect(Collectors.toList());
    }

    public List<Booking> getAllBookings() {
        return new ArrayList<>(bookings.values());
    }

    public void displayBooking(String bookingId) {
        Booking booking = bookings.get(bookingId);
        if (booking != null) {
            System.out.println("\n=== Booking Details ===");
            System.out.println("Booking ID: " + booking.getBookingId());
            System.out.println("Property ID: " + booking.getPropertyId());
            System.out.println("Guest ID: " + booking.getGuestId());
            System.out.println("Host ID: " + booking.getHostId());
            System.out.println("Check-in: " + booking.getCheckInDate());
            System.out.println("Check-out: " + booking.getCheckOutDate());
            System.out.println("Nights: " + booking.getNumberOfNights());
            System.out.println("Guests: " + booking.getNumberOfGuests());
            System.out.println("Total Amount: $" + booking.getTotalAmount());
            System.out.println("Status: " + booking.getStatus());
            System.out.println("Booked At: " + booking.getBookedAt());
            if (booking.getConfirmedAt() != null) {
                System.out.println("Confirmed At: " + booking.getConfirmedAt());
            }
        }
    }
}
