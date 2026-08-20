package com.skibooking.dto.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.skibooking.entity.Booking;
import com.skibooking.entity.enums.BookingStatus;

public record BookingResponse(
        String bookingNumber,
        BookingStatus status,
        String currency,
        BigDecimal totalAmount,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        Instant createdAt,
        List<BookingItemResponse> items) {

    public static BookingResponse from(Booking booking, List<BookingItemResponse> items) {
        return new BookingResponse(
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getCurrency(),
                booking.getTotalAmount(),
                booking.getCustomerFirstName(),
                booking.getCustomerLastName(),
                booking.getCustomerEmail(),
                booking.getCustomerPhone(),
                booking.getCreatedAt(),
                items);
    }
}
