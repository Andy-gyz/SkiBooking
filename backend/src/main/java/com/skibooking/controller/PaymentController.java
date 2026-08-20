package com.skibooking.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.payment.ConfirmPaymentRequest;
import com.skibooking.dto.payment.CreatePaymentRequest;
import com.skibooking.dto.payment.PaymentResponse;
import com.skibooking.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create")
    ResponseEntity<PaymentResponse> createPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createPayment(jwt, request.bookingNumber()));
    }

    @PostMapping("/confirm")
    PaymentResponse confirmPayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConfirmPaymentRequest request) {
        return paymentService.confirmPayment(jwt, request.bookingNumber());
    }

    @PostMapping("/webhook")
    ResponseEntity<Void> webhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        paymentService.processWebhook(payload, signature);
        return ResponseEntity.noContent().build();
    }
}
