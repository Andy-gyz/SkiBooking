package com.skibooking.web;

import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.skibooking.exception.AuthenticatedUserNotFoundException;
import com.skibooking.exception.CartItemNotFoundException;
import com.skibooking.exception.CartNotFoundException;
import com.skibooking.exception.DuplicateEmailException;
import com.skibooking.exception.InvalidCatalogRequestException;
import com.skibooking.exception.InvalidCartItemException;
import com.skibooking.exception.InvalidCredentialsException;
import com.skibooking.exception.InsufficientLessonCapacityException;
import com.skibooking.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<FieldViolation> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED",
                "Request validation failed.",
                request.getRequestURI(),
                fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "The request body is missing or malformed.",
                request);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<ApiError> handleDuplicateEmail(
            DuplicateEmailException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticatedUserNotFoundException.class)
    ResponseEntity<ApiError> handleAuthenticatedUserNotFound(
            AuthenticatedUserNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "AUTHENTICATED_USER_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCatalogRequestException.class)
    ResponseEntity<ApiError> handleInvalidCatalogRequest(
            InvalidCatalogRequestException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_CATALOG_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "Invalid value for parameter '" + exception.getName() + "'.",
                request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> handleMissingParameter(
            MissingServletRequestParameterException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER",
                "Required parameter '" + exception.getParameterName() + "' is missing.",
                request);
    }

    @ExceptionHandler(CartNotFoundException.class)
    ResponseEntity<ApiError> handleCartNotFound(
            CartNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "CART_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    ResponseEntity<ApiError> handleCartItemNotFound(
            CartItemNotFoundException exception,
            HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "CART_ITEM_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidCartItemException.class)
    ResponseEntity<ApiError> handleInvalidCartItem(
            InvalidCartItemException exception,
            HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_CART_ITEM", exception.getMessage(), request);
    }

    @ExceptionHandler(InsufficientLessonCapacityException.class)
    ResponseEntity<ApiError> handleInsufficientLessonCapacity(
            InsufficientLessonCapacityException exception,
            HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "INSUFFICIENT_LESSON_CAPACITY", exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI()));
    }
}
