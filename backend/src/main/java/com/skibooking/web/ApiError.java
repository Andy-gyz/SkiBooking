package com.skibooking.web;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors) {

    public static ApiError of(
            int status,
            String error,
            String code,
            String message,
            String path) {
        return new ApiError(Instant.now(), status, error, code, message, path, List.of());
    }
}
