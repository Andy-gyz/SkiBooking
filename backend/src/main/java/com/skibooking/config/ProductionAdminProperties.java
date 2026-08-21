package com.skibooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.production-admin")
public record ProductionAdminProperties(String email, String password) {
}
