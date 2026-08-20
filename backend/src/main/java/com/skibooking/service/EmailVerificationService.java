package com.skibooking.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.config.EmailVerificationProperties;
import com.skibooking.dto.auth.VerificationCodeResponse;
import com.skibooking.entity.EmailVerificationCode;
import com.skibooking.exception.DuplicateEmailException;
import com.skibooking.exception.VerificationCodeCooldownException;
import com.skibooking.repository.EmailVerificationCodeRepository;
import com.skibooking.repository.UserRepository;

@Service
public class EmailVerificationService {

    public enum VerificationResult {
        VALID,
        INVALID,
        EXPIRED,
        TOO_MANY_ATTEMPTS
    }

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationEmailSender emailSender;
    private final EmailVerificationProperties properties;
    private final Clock clock = Clock.systemUTC();

    public EmailVerificationService(
            EmailVerificationCodeRepository verificationCodeRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            VerificationCodeGenerator codeGenerator,
            VerificationEmailSender emailSender,
            EmailVerificationProperties properties) {
        this.verificationCodeRepository = verificationCodeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeGenerator = codeGenerator;
        this.emailSender = emailSender;
        this.properties = properties;
    }

    @Transactional
    public VerificationCodeResponse sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException();
        }

        Instant now = clock.instant();
        EmailVerificationCode verification = verificationCodeRepository.findByEmail(email)
                .orElseGet(EmailVerificationCode::new);
        if (verification.getId() != null && now.isBefore(verification.getResendAvailableAt())) {
            throw new VerificationCodeCooldownException();
        }

        String code = codeGenerator.generate();
        verification.setEmail(email);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plus(properties.codeTtl()));
        verification.setResendAvailableAt(now.plus(properties.resendCooldown()));
        verification.setAttemptCount(0);
        verification.setCreatedAt(now);
        verificationCodeRepository.saveAndFlush(verification);
        emailSender.sendVerificationCode(email, code);

        return new VerificationCodeResponse(
                "Verification code sent.",
                properties.codeTtl().toSeconds(),
                properties.resendCooldown().toSeconds());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VerificationResult verify(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        EmailVerificationCode verification = verificationCodeRepository.findByEmail(email).orElse(null);
        if (verification == null) return VerificationResult.INVALID;

        Instant now = clock.instant();
        if (now.isAfter(verification.getExpiresAt())) return VerificationResult.EXPIRED;
        if (verification.getAttemptCount() >= properties.maxAttempts()) {
            return VerificationResult.TOO_MANY_ATTEMPTS;
        }
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            verification.setAttemptCount(verification.getAttemptCount() + 1);
            verificationCodeRepository.saveAndFlush(verification);
            return verification.getAttemptCount() >= properties.maxAttempts()
                    ? VerificationResult.TOO_MANY_ATTEMPTS
                    : VerificationResult.INVALID;
        }
        return VerificationResult.VALID;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void consume(String rawEmail) {
        verificationCodeRepository.findByEmail(normalizeEmail(rawEmail))
                .ifPresent(verificationCodeRepository::delete);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
