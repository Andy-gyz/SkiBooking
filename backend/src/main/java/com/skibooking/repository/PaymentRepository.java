package com.skibooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skibooking.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripePaymentId(String stripePaymentId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}

