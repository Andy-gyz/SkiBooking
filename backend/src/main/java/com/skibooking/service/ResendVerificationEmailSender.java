package com.skibooking.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.skibooking.config.EmailVerificationProperties;
import com.skibooking.exception.EmailDeliveryException;

@Component
public class ResendVerificationEmailSender implements VerificationEmailSender {

    private static final String RESEND_EMAILS_URL = "https://api.resend.com/emails";

    private final EmailVerificationProperties properties;
    private final RestClient restClient = RestClient.create();

    public ResendVerificationEmailSender(EmailVerificationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        if (!hasText(properties.apiKey()) || !hasText(properties.from())) {
            throw new EmailDeliveryException("Email delivery is not configured.");
        }

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;padding:32px;color:#111318">
                  <div style="font-size:13px;font-weight:700;letter-spacing:.08em">SNOW ALPINE</div>
                  <h1 style="font-size:30px;margin:32px 0 12px">Verify your email</h1>
                  <p style="color:#646870;line-height:1.6">Use this code to finish creating your Snow Alpine account.</p>
                  <div style="margin:28px 0;padding:22px;border-radius:16px;background:#f1f3ff;color:#2439df;font-size:36px;font-weight:800;letter-spacing:.18em;text-align:center">%s</div>
                  <p style="color:#646870;font-size:13px;line-height:1.6">This code expires in %d minutes. If you did not request it, you can ignore this email.</p>
                </div>
                """.formatted(code, properties.codeTtl().toMinutes());

        Map<String, Object> body = Map.of(
                "from", properties.from(),
                "to", List.of(email),
                "subject", "Your Snow Alpine verification code",
                "html", html);
        try {
            restClient.post()
                    .uri(RESEND_EMAILS_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new EmailDeliveryException("We could not send the verification email. Please try again.", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
