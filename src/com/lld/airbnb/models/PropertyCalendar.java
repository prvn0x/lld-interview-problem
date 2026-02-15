package com.lld.airbnb.models;

import java.time.LocalDate;
import java.util.*;

public class PropertyCalendar {
    private String propertyId;
    private Set<DateRange> blockedDateRanges;

    public PropertyCalendar(String propertyId) {
        this.propertyId = propertyId;
        this.blockedDateRanges = new HashSet<>();
    }

    // Check if property is available for given date range
    public boolean isAvailable(LocalDate checkIn, LocalDate checkOut) {
        DateRange requestedRange = new DateRange(checkIn, checkOut);

        for (DateRange blocked : blockedDateRanges) {
            if (requestedRange.overlaps(blocked)) {
                return false;
            }
        }
        return true;
    }

    // Block dates for a booking
    public void blockDates(LocalDate checkIn, LocalDate checkOut) {
        blockedDateRanges.add(new DateRange(checkIn, checkOut));
    }

    // Unblock dates (when booking is cancelled)
    public void unblockDates(LocalDate checkIn, LocalDate checkOut) {
        blockedDateRanges.removeIf(range ->
                range.getStartDate().equals(checkIn) && range.getEndDate().equals(checkOut)
        );
    }

    public String getPropertyId() { return propertyId; }
    public Set<DateRange> getBlockedDateRanges() { return blockedDateRanges; }

    // Inner class to represent a date range
    public static class DateRange {
        private LocalDate startDate;
        private LocalDate endDate;

        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        // Check if this date range overlaps with another
        public boolean overlaps(DateRange other) {
            // Two ranges overlap unless one ends before the other starts
            return !(this.endDate.isBefore(other.startDate) ||
                    this.startDate.isAfter(other.endDate));
        }

        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DateRange dateRange = (DateRange) o;
            return Objects.equals(startDate, dateRange.startDate) &&
                   Objects.equals(endDate, dateRange.endDate);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startDate, endDate);
        }

        @Override
        public String toString() {
            return startDate + " to " + endDate;
        }
    }
}
