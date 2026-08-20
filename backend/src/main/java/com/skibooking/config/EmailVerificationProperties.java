package com.skibooking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.email-verification")
public record EmailVerificationProperties(
        String apiKey,
        String from,
        Duration codeTtl,
        Duration resendCooldown,
        int maxAttempts) {
}
