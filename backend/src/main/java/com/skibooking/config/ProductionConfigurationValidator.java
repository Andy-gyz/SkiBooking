package com.skibooking.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionConfigurationValidator implements ApplicationRunner {

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "DB_URL",
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET",
            "CORS_ALLOWED_ORIGINS",
            "STRIPE_SECRET_KEY",
            "STRIPE_WEBHOOK_SECRET",
            "RESEND_API_KEY",
            "EMAIL_FROM",
            "INITIAL_ADMIN_EMAIL",
            "INITIAL_ADMIN_PASSWORD");

    private final Environment environment;

    public ProductionConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> missing = REQUIRED_VARIABLES.stream()
                .filter(name -> !hasProductionValue(environment.getProperty(name)))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Production configuration is missing required environment variables: "
                            + String.join(", ", missing));
        }

        String corsOrigins = environment.getRequiredProperty("CORS_ALLOWED_ORIGINS");
        if (corsOrigins.contains("localhost") || !corsOrigins.contains("https://snowalpineresort.com")) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS must include https://snowalpineresort.com and must not include localhost in production.");
        }

        String emailFrom = environment.getRequiredProperty("EMAIL_FROM");
        if (!emailFrom.toLowerCase().contains("snowalpineresort.com")) {
            throw new IllegalStateException(
                    "EMAIL_FROM must use a verified snowalpineresort.com sender in production.");
        }

        String adminPassword = environment.getRequiredProperty("INITIAL_ADMIN_PASSWORD");
        if (adminPassword.length() < 12) {
            throw new IllegalStateException("INITIAL_ADMIN_PASSWORD must contain at least 12 characters.");
        }
    }

    private boolean hasProductionValue(String value) {
        return value != null
                && !value.isBlank()
                && !value.contains("replace_me")
                && !value.contains("replace-with-");
    }
}
