package com.skibooking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.dto.payment.PaymentResponse;
import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Payment;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.PaymentStatus;
import com.skibooking.exception.AuthenticatedUserNotFoundException;
import com.skibooking.exception.BookingNotFoundException;
import com.skibooking.exception.InvalidPaymentException;
import com.skibooking.exception.InvalidStripeWebhookException;
import com.skibooking.exception.PaymentNotFoundException;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.BookingRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Set<String> PAYMENT_INTENT_EVENTS = Set.of(
            "payment_intent.processing",
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "payment_intent.canceled");

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final LessonSessionRepository lessonSessionRepository;
    private final PaymentRepository paymentRepository;
    private final StripePaymentGateway stripePaymentGateway;

    public PaymentService(
            BookingRepository bookingRepository,
            BookingItemRepository bookingItemRepository,
            LessonSessionRepository lessonSessionRepository,
            PaymentRepository paymentRepository,
            StripePaymentGateway stripePaymentGateway) {
        this.bookingRepository = bookingRepository;
        this.bookingItemRepository = bookingItemRepository;
        this.lessonSessionRepository = lessonSessionRepository;
        this.paymentRepository = paymentRepository;
        this.stripePaymentGateway = stripePaymentGateway;
    }

    @Transactional
    public PaymentResponse createPayment(Jwt jwt, String bookingNumber) {
        Booking booking = findOwnedPendingBookingForUpdate(jwt, bookingNumber);
        Payment existingPayment = paymentRepository
                .findFirstByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElse(null);
        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.SUCCEEDED) {
                throw new InvalidPaymentException("This booking has already been paid.");
            }
            StripePaymentIntent intent = stripePaymentGateway
                    .retrievePaymentIntent(existingPayment.getStripePaymentId());
            validateStripeIntent(existingPayment, intent);
            synchronizePayment(existingPayment, intent);
            return PaymentResponse.from(booking, existingPayment, intent.clientSecret());
        }

        StripePaymentIntent intent = stripePaymentGateway.createPaymentIntent(
                booking.getBookingNumber(), booking.getTotalAmount(), booking.getCurrency());
        if (!hasText(intent.id()) || !hasText(intent.clientSecret())) {
            throw new InvalidPaymentException("Stripe returned an incomplete payment response.");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStripePaymentId(intent.id());
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency(booking.getCurrency());
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.saveAndFlush(payment);
        validateStripeIntent(payment, intent);
        return PaymentResponse.from(booking, payment, intent.clientSecret());
    }

    @Transactional
    public PaymentResponse confirmPayment(Jwt jwt, String bookingNumber) {
        Booking booking = bookingRepository.findOwnedByBookingNumberForUpdate(
                        bookingNumber, authenticatedUserId(jwt))
                .orElseThrow(BookingNotFoundException::new);
        Payment payment = paymentRepository.findFirstByBookingIdOrderByCreatedAtDesc(booking.getId())
                .orElseThrow(PaymentNotFoundException::new);
        StripePaymentIntent intent = stripePaymentGateway
                .retrievePaymentIntent(payment.getStripePaymentId());
        validateStripeIntent(payment, intent);
        synchronizePayment(payment, intent);
        return PaymentResponse.from(booking, payment, null);
    }

    @Transactional
    public void processWebhook(String payload, String signature) {
        StripeWebhookEvent event = stripePaymentGateway.verifyWebhook(payload, signature);
        if (!PAYMENT_INTENT_EVENTS.contains(event.type())) {
            return;
        }
        if (event.paymentIntent() == null || !hasText(event.paymentIntent().id())) {
            throw new InvalidStripeWebhookException();
        }
        Payment payment = paymentRepository
                .findByStripePaymentIdForUpdate(event.paymentIntent().id())
                .orElse(null);
        if (payment == null) {
            return;
        }
        validateStripeIntent(payment, event.paymentIntent());
        applyEvent(payment, event.type());
    }

    private Booking findOwnedPendingBookingForUpdate(Jwt jwt, String bookingNumber) {
        Booking booking = bookingRepository.findOwnedByBookingNumberForUpdate(
                        bookingNumber, authenticatedUserId(jwt))
                .orElseThrow(BookingNotFoundException::new);
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalidPaymentException("Only a pending booking can start payment.");
        }
        return booking;
    }

    private void synchronizePayment(Payment payment, StripePaymentIntent intent) {
        switch (intent.status()) {
            case "succeeded" -> markSucceeded(payment);
            case "canceled" -> cancelBookingAndReleaseCapacity(payment);
            default -> {
                if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                    payment.setStatus(PaymentStatus.PENDING);
                }
            }
        }
    }

    private void applyEvent(Payment payment, String eventType) {
        switch (eventType) {
            case "payment_intent.succeeded" -> markSucceeded(payment);
            case "payment_intent.payment_failed" -> markFailedAttempt(payment);
            case "payment_intent.canceled" -> cancelBookingAndReleaseCapacity(payment);
            case "payment_intent.processing" -> {
                if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
                    payment.setStatus(PaymentStatus.PENDING);
                }
            }
            default -> {
                // The event allowlist makes this branch unreachable.
            }
        }
    }

    private void markSucceeded(Payment payment) {
        Booking booking = payment.getBooking();
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidPaymentException("A cancelled booking cannot be confirmed.");
        }
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaymentMethod("card");
        if (payment.getPaidAt() == null) {
            payment.setPaidAt(Instant.now());
        }
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }
    }

    private void markFailedAttempt(Payment payment) {
        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            payment.setStatus(PaymentStatus.FAILED);
        }
    }

    private void cancelBookingAndReleaseCapacity(Payment payment) {
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            return;
        }
        Booking booking = payment.getBooking();
        payment.setStatus(PaymentStatus.FAILED);
        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }
        for (BookingItem item : bookingItemRepository.findByBookingIdOrderByIdAsc(booking.getId())) {
            if (item.getLessonSession() == null) {
                continue;
            }
            LessonSession session = lessonSessionRepository.findByIdForUpdate(item.getLessonSession().getId())
                    .orElseThrow(() -> new InvalidPaymentException(
                            "The reserved lesson session no longer exists."));
            int remainingBookedCount = session.getBookedCount() - item.getQuantity();
            if (remainingBookedCount < 0) {
                throw new InvalidPaymentException("Lesson capacity accounting is inconsistent.");
            }
            session.setBookedCount(remainingBookedCount);
        }
        booking.setStatus(BookingStatus.CANCELLED);
    }

    private void validateStripeIntent(Payment payment, StripePaymentIntent intent) {
        if (intent == null
                || intent.amountMinor() == null
                || !payment.getStripePaymentId().equals(intent.id())
                || !payment.getCurrency().equalsIgnoreCase(intent.currency())
                || toMinorUnits(payment.getAmount()) != intent.amountMinor()) {
            throw new InvalidPaymentException("Stripe payment details do not match the booking.");
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact();
    }

    private long authenticatedUserId(Jwt jwt) {
        Number userId = jwt.getClaim("uid");
        if (userId == null) {
            throw new AuthenticatedUserNotFoundException();
        }
        return userId.longValue();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
