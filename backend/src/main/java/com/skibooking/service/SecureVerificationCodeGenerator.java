package com.skibooking.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class SecureVerificationCodeGenerator implements VerificationCodeGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        return "%06d".formatted(secureRandom.nextInt(1_000_000));
    }
}
