package com.skibooking.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.BookingRepository;

@Service
public class BookingConfirmationEmailService {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final BookingConfirmationEmailSender emailSender;

    public BookingConfirmationEmailService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            BookingConfirmationEmailSender emailSender) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.emailSender = emailSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfPending(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId).orElse(null);
        if (booking == null
                || booking.getStatus() != BookingStatus.CONFIRMED
                || booking.getConfirmationEmailSentAt() != null) {
            return;
        }
        List<BookingItem> items = bookingItemRepository.findByBookingIdOrderByIdAsc(bookingId);
        emailSender.sendConfirmation(booking, items);
        booking.setConfirmationEmailSentAt(Instant.now());
    }
}
