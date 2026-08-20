package com.skibooking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.EmailVerificationCode;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {

    Optional<EmailVerificationCode> findByEmail(String email);
}
