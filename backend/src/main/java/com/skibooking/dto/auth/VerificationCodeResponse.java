package com.skibooking.dto.auth;

public record VerificationCodeResponse(
        String message,
        long expiresInSeconds,
        long resendAfterSeconds) {
}
