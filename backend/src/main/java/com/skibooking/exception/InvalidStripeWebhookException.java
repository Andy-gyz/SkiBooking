package com.skibooking.exception;

public class InvalidStripeWebhookException extends RuntimeException {

    public InvalidStripeWebhookException() {
        super("The Stripe webhook signature is invalid.");
    }
}
