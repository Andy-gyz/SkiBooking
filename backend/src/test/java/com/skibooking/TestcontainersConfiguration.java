package com.skibooking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("skibooking_test")
                .withUsername("skibooking")
                .withPassword("skibooking_test");
    }

    @Bean
    DynamicPropertyRegistrar jwtTestProperties() {
        return registry -> registry.add(
                "app.security.jwt.secret",
                () -> "test-only-jwt-secret-that-is-longer-than-32-characters");
    }
}
