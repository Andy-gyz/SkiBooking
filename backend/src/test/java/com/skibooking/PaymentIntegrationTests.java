package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.PaymentStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.entity.enums.UserRole;
import com.skibooking.exception.InvalidStripeWebhookException;
import com.skibooking.repository.BookingItemRepository;
import com.skibooking.repository.BookingRepository;
import com.skibooking.repository.CartItemRepository;
import com.skibooking.repository.CartRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.PaymentRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;
import com.skibooking.repository.UserRepository;
import com.skibooking.service.JwtService;
import com.skibooking.service.BookingConfirmationEmailSender;
import com.skibooking.service.StripePaymentGateway;
import com.skibooking.service.StripePaymentIntent;
import com.skibooking.service.StripeWebhookEvent;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingItemRepository bookingItemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private LessonSessionRepository lessonSessionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ResortRepository resortRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private StripePaymentGateway stripePaymentGateway;

    @MockitoBean
    private BookingConfirmationEmailSender bookingConfirmationEmailSender;

    private User owner;
    private String ownerToken;
    private Booking booking;
    private LessonSession lessonSession;

    @BeforeEach
    void setUp() {
        clearDatabase();
        reset(stripePaymentGateway, bookingConfirmationEmailSender);

        owner = createUser("owner@example.com");
        ownerToken = jwtService.createAuthResponse(owner, null).accessToken();
        Resort resort = createResort();
        Product lesson = createLessonProduct(resort);
        lessonSession = createLessonSession(lesson);
        booking = createPendingBooking(owner, lesson, lessonSession);
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    void createPaymentRequiresOwnerAndReusesOnePaymentIntent() throws Exception {
        User otherUser = createUser("other@example.com");
        String otherToken = jwtService.createAuthResponse(otherUser, null).accessToken();
        StripePaymentIntent intent = intent("requires_payment_method");
        when(stripePaymentGateway.createPaymentIntent(
                        booking.getBookingNumber(), new BigDecimal("240.00"), "AUD"))
                .thenReturn(intent);
        when(stripePaymentGateway.retrievePaymentIntent("pi_test_123")).thenReturn(intent);

        mockMvc.perform(post("/api/payments/create")
                        .header("Authorization", bearer(otherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));

        createPayment(ownerToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"))
                .andExpect(jsonPath("$.clientSecret").value("pi_test_123_secret_test"));
        createPayment(ownerToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentIntentId").value("pi_test_123"));

        assertThat(paymentRepository.count()).isEqualTo(1);
        verify(stripePaymentGateway, times(1)).createPaymentIntent(
                booking.getBookingNumber(), new BigDecimal("240.00"), "AUD");
    }

    @Test
    void succeededWebhookConfirmsBookingAndIsIdempotent() throws Exception {
        createStoredPayment();
        StripeWebhookEvent event = new StripeWebhookEvent(
                "payment_intent.succeeded", intent("succeeded"));
        when(stripePaymentGateway.verifyWebhook("payload", "signature")).thenReturn(event);

        sendWebhook().andExpect(status().isNoContent());
        sendWebhook().andExpect(status().isNoContent());

        Booking confirmed = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(confirmed.getConfirmationEmailSentAt()).isNotNull();
        assertThat(paymentRepository.findByStripePaymentId("pi_test_123").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(paymentRepository.findByStripePaymentId("pi_test_123").orElseThrow().getPaidAt())
                .isNotNull();
        assertThat(lessonSessionRepository.findById(lessonSession.getId()).orElseThrow().getBookedCount())
                .isEqualTo(2);
        verify(bookingConfirmationEmailSender, times(1)).sendConfirmation(any(), anyList());
    }

    @Test
    void failedAttemptStaysPendingAndCanLaterSucceed() throws Exception {
        createStoredPayment();
        when(stripePaymentGateway.verifyWebhook("payload", "signature"))
                .thenReturn(new StripeWebhookEvent(
                        "payment_intent.payment_failed", intent("requires_payment_method")))
                .thenReturn(new StripeWebhookEvent(
                        "payment_intent.succeeded", intent("succeeded")));

        sendWebhook().andExpect(status().isNoContent());
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.PENDING);
        assertThat(paymentRepository.findByStripePaymentId("pi_test_123").orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.FAILED);

        sendWebhook().andExpect(status().isNoContent());
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void canceledPaymentReleasesLessonCapacityExactlyOnce() throws Exception {
        createStoredPayment();
        StripeWebhookEvent event = new StripeWebhookEvent(
                "payment_intent.canceled", intent("canceled"));
        when(stripePaymentGateway.verifyWebhook("payload", "signature")).thenReturn(event);

        sendWebhook().andExpect(status().isNoContent());
        sendWebhook().andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.CANCELLED);
        assertThat(lessonSessionRepository.findById(lessonSession.getId()).orElseThrow().getBookedCount())
                .isZero();
    }

    @Test
    void confirmReadsStripeStatusInsteadOfTrustingTheClient() throws Exception {
        createStoredPayment();
        when(stripePaymentGateway.retrievePaymentIntent("pi_test_123"))
                .thenReturn(intent("succeeded"));

        mockMvc.perform(post("/api/payments/confirm")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$.clientSecret").doesNotExist());
    }

    @Test
    void invalidWebhookSignatureIsRejected() throws Exception {
        when(stripePaymentGateway.verifyWebhook("payload", "bad-signature"))
                .thenThrow(new InvalidStripeWebhookException());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "bad-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("payload"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STRIPE_WEBHOOK"));
    }

    private org.springframework.test.web.servlet.ResultActions createPayment(String token) throws Exception {
        return mockMvc.perform(post("/api/payments/create")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentRequest()));
    }

    private org.springframework.test.web.servlet.ResultActions sendWebhook() throws Exception {
        return mockMvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content("payload"));
    }

    private void createStoredPayment() {
        com.skibooking.entity.Payment payment = new com.skibooking.entity.Payment();
        payment.setBooking(booking);
        payment.setStripePaymentId("pi_test_123");
        payment.setAmount(new BigDecimal("240.00"));
        payment.setCurrency("AUD");
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.saveAndFlush(payment);
    }

    private StripePaymentIntent intent(String status) {
        return new StripePaymentIntent(
                "pi_test_123",
                "pi_test_123_secret_test",
                status,
                24000L,
                "aud",
                status.equals("succeeded") ? "pm_test_123" : null);
    }

    private String paymentRequest() {
        return """
                {"bookingNumber":"%s"}
                """.formatted(booking.getBookingNumber());
    }

    private Booking createPendingBooking(User user, Product lesson, LessonSession session) {
        Booking newBooking = new Booking();
        newBooking.setBookingNumber("SKI-20260820-PAYMENTTEST1");
        newBooking.setUser(user);
        newBooking.setCustomerFirstName("Payment");
        newBooking.setCustomerLastName("Tester");
        newBooking.setCustomerEmail(user.getEmail());
        newBooking.setStatus(BookingStatus.PENDING);
        newBooking.setCurrency("AUD");
        newBooking.setTotalAmount(new BigDecimal("240.00"));
        bookingRepository.saveAndFlush(newBooking);

        BookingItem item = new BookingItem();
        item.setBooking(newBooking);
        item.setProduct(lesson);
        item.setLessonSession(session);
        item.setProductName(lesson.getName());
        item.setCategory(ProductCategory.LESSON);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("120.00"));
        item.setSubtotal(new BigDecimal("240.00"));
        bookingItemRepository.saveAndFlush(item);
        return newBooking;
    }

    private LessonSession createLessonSession(Product lesson) {
        LessonSession session = new LessonSession();
        session.setProduct(lesson);
        session.setSessionDate(LocalDate.now().plusDays(5));
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setCapacity(4);
        session.setBookedCount(2);
        session.setStatus(LessonSessionStatus.ACTIVE);
        return lessonSessionRepository.saveAndFlush(session);
    }

    private Product createLessonProduct(Resort resort) {
        Product product = new Product();
        product.setResort(resort);
        product.setName("Beginner Ski Lesson");
        product.setCategory(ProductCategory.LESSON);
        product.setPrice(new BigDecimal("120.00"));
        product.setActive(true);
        return productRepository.saveAndFlush(product);
    }

    private Resort createResort() {
        Resort resort = new Resort();
        resort.setName("Snow Alpine Resort");
        resort.setLocation("Victoria, Australia");
        resort.setStatus(ResortStatus.ACTIVE);
        return resortRepository.saveAndFlush(resort);
    }

    private User createUser(String email) {
        User user = new User();
        user.setFirstName("Payment");
        user.setLastName("Tester");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.CUSTOMER);
        return userRepository.saveAndFlush(user);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private void clearDatabase() {
        paymentRepository.deleteAll();
        bookingItemRepository.deleteAll();
        bookingRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        lessonSessionRepository.deleteAll();
        productRepository.deleteAll();
        resortRepository.deleteAll();
        userRepository.deleteAll();
    }
}
