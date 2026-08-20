package com.skibooking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.skibooking.config.StripeProperties;
import com.skibooking.exception.InvalidPaymentException;
import com.skibooking.exception.InvalidStripeWebhookException;
import com.skibooking.exception.StripeServiceException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;

@Component
public class StripePaymentGatewayImpl implements StripePaymentGateway {

    private final StripeProperties properties;

    public StripePaymentGatewayImpl(StripeProperties properties) {
        this.properties = properties;
    }

    @Override
    public StripePaymentIntent createPaymentIntent(
            String bookingNumber,
            BigDecimal amount,
            String currency) {
        requireApiKey();
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorUnits(amount))
                    .setCurrency(currency.toLowerCase(Locale.ROOT))
                    .addPaymentMethodType("card")
                    .putMetadata("booking_number", bookingNumber)
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setApiKey(properties.apiKey())
                    .setIdempotencyKey("skibooking-payment-" + bookingNumber)
                    .build();
            return toDomain(PaymentIntent.create(params, options));
        } catch (StripeException exception) {
            throw new StripeServiceException("Stripe could not create the payment.", exception);
        }
    }

    @Override
    public StripePaymentIntent retrievePaymentIntent(String paymentIntentId) {
        requireApiKey();
        try {
            RequestOptions options = RequestOptions.builder()
                    .setApiKey(properties.apiKey())
                    .build();
            return toDomain(PaymentIntent.retrieve(paymentIntentId, options));
        } catch (StripeException exception) {
            throw new StripeServiceException("Stripe could not retrieve the payment.", exception);
        }
    }

    @Override
    public StripeWebhookEvent verifyWebhook(String payload, String signature) {
        if (!hasText(properties.webhookSecret())) {
            throw new InvalidPaymentException("Stripe webhook configuration is missing.");
        }
        try {
            Event event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
            StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
            if (stripeObject == null) {
                stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
            }
            StripePaymentIntent paymentIntent = stripeObject instanceof PaymentIntent intent
                    ? toDomain(intent)
                    : null;
            return new StripeWebhookEvent(event.getType(), paymentIntent);
        } catch (SignatureVerificationException
                | EventDataObjectDeserializationException
                | IllegalArgumentException exception) {
            throw new InvalidStripeWebhookException();
        }
    }

    private StripePaymentIntent toDomain(PaymentIntent paymentIntent) {
        return new StripePaymentIntent(
                paymentIntent.getId(),
                paymentIntent.getClientSecret(),
                paymentIntent.getStatus(),
                paymentIntent.getAmount(),
                paymentIntent.getCurrency(),
                paymentIntent.getPaymentMethod());
    }

    private long toMinorUnits(BigDecimal amount) {
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new InvalidPaymentException("The booking total cannot be converted to AUD cents.");
        }
    }

    private void requireApiKey() {
        if (!hasText(properties.apiKey())) {
            throw new InvalidPaymentException("Stripe API configuration is missing.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
