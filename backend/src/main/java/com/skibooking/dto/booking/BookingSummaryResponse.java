package com.skibooking.dto.booking;

import java.math.BigDecimal;
import java.time.Instant;

import com.skibooking.entity.Booking;
import com.skibooking.entity.enums.BookingStatus;

public record BookingSummaryResponse(
        String bookingNumber,
        BookingStatus status,
        String currency,
        BigDecimal totalAmount,
        int itemCount,
        Instant createdAt) {

    public static BookingSummaryResponse from(Booking booking, int itemCount) {
        return new BookingSummaryResponse(
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getCurrency(),
                booking.getTotalAmount(),
                itemCount,
                booking.getCreatedAt());
    }
}
