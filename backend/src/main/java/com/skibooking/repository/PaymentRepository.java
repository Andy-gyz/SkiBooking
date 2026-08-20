package com.skibooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skibooking.entity.Payment;

import jakarta.persistence.LockModeType;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByStripePaymentId(String stripePaymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from Payment payment where payment.stripePaymentId = :stripePaymentId")
    Optional<Payment> findByStripePaymentIdForUpdate(
            @Param("stripePaymentId") String stripePaymentId);

    Optional<Payment> findFirstByBookingIdOrderByCreatedAtDesc(Long bookingId);

    List<Payment> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
