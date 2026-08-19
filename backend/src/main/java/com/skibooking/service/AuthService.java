package com.skibooking.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.auth.AuthResponse;
import com.skibooking.dto.auth.LoginRequest;
import com.skibooking.dto.auth.RegisterRequest;
import com.skibooking.dto.auth.UserResponse;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.UserRole;
import com.skibooking.exception.AuthenticatedUserNotFoundException;
import com.skibooking.exception.DuplicateEmailException;
import com.skibooking.exception.InvalidCredentialsException;
import com.skibooking.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(normalizeOptional(request.phone()));
        user.setRole(UserRole.CUSTOMER);

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
        return jwtService.createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return jwtService.createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Jwt jwt) {
        Number userId = jwt.getClaim("uid");
        if (userId == null) {
            throw new AuthenticatedUserNotFoundException();
        }
        User user = userRepository.findById(userId.longValue())
                .orElseThrow(AuthenticatedUserNotFoundException::new);
        return UserResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
