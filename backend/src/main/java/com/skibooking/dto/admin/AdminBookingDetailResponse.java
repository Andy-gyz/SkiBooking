package com.skibooking.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.skibooking.dto.booking.BookingItemResponse;
import com.skibooking.entity.Booking;
import com.skibooking.entity.enums.BookingStatus;

public record AdminBookingDetailResponse(
        Long id,
        String bookingNumber,
        BookingStatus status,
        String currency,
        BigDecimal totalAmount,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        Instant createdAt,
        Instant updatedAt,
        List<BookingItemResponse> items,
        List<AdminPaymentResponse> payments) {

    public static AdminBookingDetailResponse from(
            Booking booking,
            List<BookingItemResponse> items,
            List<AdminPaymentResponse> payments) {
        return new AdminBookingDetailResponse(
                booking.getId(),
                booking.getBookingNumber(),
                booking.getStatus(),
                booking.getCurrency(),
                booking.getTotalAmount(),
                booking.getCustomerFirstName(),
                booking.getCustomerLastName(),
                booking.getCustomerEmail(),
                booking.getCustomerPhone(),
                booking.getCreatedAt(),
                booking.getUpdatedAt(),
                items,
                payments);
    }
}
