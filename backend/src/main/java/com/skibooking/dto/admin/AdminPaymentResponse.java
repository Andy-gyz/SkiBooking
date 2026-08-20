package com.skibooking.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

import com.skibooking.entity.Payment;
import com.skibooking.entity.enums.PaymentStatus;

public record AdminPaymentResponse(
        Long id,
        String stripePaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String paymentMethod,
        Instant paidAt,
        Instant createdAt) {

    public static AdminPaymentResponse from(Payment payment) {
        return new AdminPaymentResponse(
                payment.getId(),
                payment.getStripePaymentId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getPaidAt(),
                payment.getCreatedAt());
    }
}
