package com.skibooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.local-admin")
public record LocalAdminProperties(String email, String password) {
}
