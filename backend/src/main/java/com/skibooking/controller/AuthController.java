package com.skibooking.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skibooking.dto.auth.AuthResponse;
import com.skibooking.dto.auth.LoginRequest;
import com.skibooking.dto.auth.RegisterRequest;
import com.skibooking.dto.auth.SendVerificationCodeRequest;
import com.skibooking.dto.auth.UserResponse;
import com.skibooking.dto.auth.VerificationCodeResponse;
import com.skibooking.service.AuthService;
import com.skibooking.service.EmailVerificationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/verification-codes")
    VerificationCodeResponse sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        return emailVerificationService.sendCode(request.email());
    }

    @PostMapping("/register")
    ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(jwt);
    }
}
