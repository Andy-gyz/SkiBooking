package com.skibooking.dto.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        Long cartId) {
}
