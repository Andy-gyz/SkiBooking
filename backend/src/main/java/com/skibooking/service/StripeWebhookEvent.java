package com.skibooking.service;

public record StripeWebhookEvent(
        String type,
        StripePaymentIntent paymentIntent) {
}
