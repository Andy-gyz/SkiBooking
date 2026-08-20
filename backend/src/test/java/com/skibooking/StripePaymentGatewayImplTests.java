package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.skibooking.config.StripeProperties;
import com.skibooking.exception.InvalidStripeWebhookException;
import com.skibooking.service.StripePaymentGatewayImpl;
import com.skibooking.service.StripeWebhookEvent;

class StripePaymentGatewayImplTests {

    private static final String WEBHOOK_SECRET = "whsec_test_signing_secret";

    private final StripePaymentGatewayImpl gateway = new StripePaymentGatewayImpl(
            new StripeProperties("sk_test_not_used", WEBHOOK_SECRET));

    @Test
    void verifiesTheRawPayloadAndDeserializesAPaymentIntent() throws Exception {
        String payload = """
                {
                  "id": "evt_test_123",
                  "object": "event",
                  "api_version": "2020-08-27",
                  "type": "payment_intent.succeeded",
                  "data": {
                    "object": {
                      "id": "pi_test_123",
                      "object": "payment_intent",
                      "amount": 24000,
                      "currency": "aud",
                      "client_secret": "pi_test_123_secret_test",
                      "status": "succeeded",
                      "payment_method": "pm_test_123"
                    }
                  }
                }
                """;
        long timestamp = Instant.now().getEpochSecond();
        String signature = stripeSignature(payload, timestamp);

        StripeWebhookEvent event = gateway.verifyWebhook(payload, signature);

        assertThat(event.type()).isEqualTo("payment_intent.succeeded");
        assertThat(event.paymentIntent().id()).isEqualTo("pi_test_123");
        assertThat(event.paymentIntent().amountMinor()).isEqualTo(24000L);
        assertThat(event.paymentIntent().currency()).isEqualTo("aud");
    }

    @Test
    void rejectsASignatureForADifferentPayload() throws Exception {
        String payload = "{\"id\":\"evt_test_123\",\"object\":\"event\"}";
        long timestamp = Instant.now().getEpochSecond();
        String signature = stripeSignature(payload, timestamp);

        assertThatThrownBy(() -> gateway.verifyWebhook(payload + " ", signature))
                .isInstanceOf(InvalidStripeWebhookException.class);
    }

    private String stripeSignature(String payload, long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + toHex(digest);
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value));
        }
        return result.toString();
    }
}
