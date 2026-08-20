package com.skibooking.dto.admin;

import java.time.Instant;

import com.skibooking.dto.booking.BookingItemResponse;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.Payment;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.PaymentStatus;

public record AdminReservationResponse(
        Long bookingId,
        String bookingNumber,
        BookingStatus bookingStatus,
        Instant createdAt,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        PaymentStatus paymentStatus,
        BookingItemResponse item) {

    public static AdminReservationResponse from(BookingItem item, Payment payment) {
        return new AdminReservationResponse(
                item.getBooking().getId(),
                item.getBooking().getBookingNumber(),
                item.getBooking().getStatus(),
                item.getBooking().getCreatedAt(),
                item.getBooking().getCustomerFirstName(),
                item.getBooking().getCustomerLastName(),
                item.getBooking().getCustomerEmail(),
                item.getBooking().getCustomerPhone(),
                payment == null ? null : payment.getStatus(),
                BookingItemResponse.from(item));
    }
}
