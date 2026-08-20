package com.skibooking.service;

public record StripePaymentIntent(
        String id,
        String clientSecret,
        String status,
        Long amountMinor,
        String currency,
        String paymentMethod) {
}
