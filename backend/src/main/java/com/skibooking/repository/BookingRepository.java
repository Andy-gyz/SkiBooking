package com.skibooking.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.skibooking.entity.Booking;

import jakarta.persistence.LockModeType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    Optional<Booking> findByBookingNumberAndUserId(String bookingNumber, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select booking from Booking booking
            where booking.bookingNumber = :bookingNumber
              and booking.user.id = :userId
            """)
    Optional<Booking> findOwnedByBookingNumberForUpdate(
            @Param("bookingNumber") String bookingNumber,
            @Param("userId") Long userId);

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);
}
