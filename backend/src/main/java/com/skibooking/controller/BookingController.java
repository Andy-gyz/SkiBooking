package com.skibooking.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.booking.BookingResponse;
import com.skibooking.dto.booking.BookingSummaryResponse;
import com.skibooking.dto.booking.CreateBookingRequest;
import com.skibooking.service.BookingService;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/bookings")
    ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateBookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(jwt, request));
    }

    @GetMapping("/bookings/{bookingNumber}")
    BookingResponse getBooking(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String bookingNumber) {
        return bookingService.getBooking(jwt, bookingNumber);
    }

    @GetMapping("/my-bookings")
    List<BookingSummaryResponse> listMyBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.listMyBookings(jwt);
    }
}
