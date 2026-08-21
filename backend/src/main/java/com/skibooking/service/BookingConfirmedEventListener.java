package com.skibooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class BookingConfirmedEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookingConfirmedEventListener.class);

    private final BookingConfirmationEmailService emailService;

    public BookingConfirmedEventListener(BookingConfirmationEmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        try {
            emailService.sendIfPending(event.bookingId());
        } catch (RuntimeException exception) {
            LOGGER.warn("Booking confirmation email delivery failed for booking {}.",
                    event.bookingId(), exception);
        }
    }
}
