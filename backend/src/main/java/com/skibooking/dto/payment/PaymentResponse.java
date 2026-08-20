package com.skibooking.dto.payment;

import java.math.BigDecimal;

import com.skibooking.entity.Booking;
import com.skibooking.entity.Payment;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.PaymentStatus;

public record PaymentResponse(
        String bookingNumber,
        BookingStatus bookingStatus,
        String paymentIntentId,
        PaymentStatus paymentStatus,
        BigDecimal amount,
        String currency,
        String clientSecret) {

    public static PaymentResponse from(Booking booking, Payment payment, String clientSecret) {
        return new PaymentResponse(
                booking.getBookingNumber(),
                booking.getStatus(),
                payment.getStripePaymentId(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                clientSecret);
    }
}
