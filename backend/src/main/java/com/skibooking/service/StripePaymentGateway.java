package com.skibooking.service;

import java.math.BigDecimal;

public interface StripePaymentGateway {

    StripePaymentIntent createPaymentIntent(
            String bookingNumber,
            BigDecimal amount,
            String currency);

    StripePaymentIntent retrievePaymentIntent(String paymentIntentId);

    StripeWebhookEvent verifyWebhook(String payload, String signature);
}
