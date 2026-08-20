package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.skibooking.entity.Booking;
import com.skibooking.entity.BookingItem;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Payment;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.BookingStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.PaymentStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.entity.enums.UserRole;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private Resort resort;
    private Product resortAccess;
    private Product liftTicket;
    private Product lesson;
    private Product rental;
    private LessonSession reservedSession;
    private User customer;
    private String adminToken;
    private String customerToken;
    private Booking confirmedBooking;

    @BeforeEach
    void setUp() {
        clearDatabase();
        User admin = createUser("admin@example.com", UserRole.ADMIN);
        customer = createUser("customer@example.com", UserRole.CUSTOMER);
        adminToken = jwtService.createAuthResponse(admin, null).accessToken();
        customerToken = jwtService.createAuthResponse(customer, null).accessToken();

        resort = createResort();
        resortAccess = createProduct("Daily Vehicle Entry", ProductCategory.RESORT_ACCESS, "55.00");
        liftTicket = createProduct("Adult Full Day Lift Pass", ProductCategory.LIFT_TICKET, "135.00");
        lesson = createProduct("Beginner Ski Lesson", ProductCategory.LESSON, "120.00");
        rental = createProduct("Ski Package", ProductCategory.RENTAL, "65.00");
        reservedSession = createLessonSession(lesson, 4, 1, LocalDate.now().plusDays(10));

        confirmedBooking = createBooking("ADMIN-CONFIRMED", BookingStatus.CONFIRMED);
        addItem(confirmedBooking, resortAccess, 2, null);
        addItem(confirmedBooking, liftTicket, 3, null);
        addItem(confirmedBooking, lesson, 1, reservedSession);
        addItem(confirmedBooking, rental, 4, null);
        addSucceededPayment(confirmedBooking);

        Booking completed = createBooking("ADMIN-COMPLETED", BookingStatus.COMPLETED);
        addItem(completed, liftTicket, 2, null);

        Booking pending = createBooking("ADMIN-PENDING", BookingStatus.PENDING);
        addItem(pending, liftTicket, 20, null);

        Booking cancelled = createBooking("ADMIN-CANCELLED", BookingStatus.CANCELLED);
        addItem(cancelled, rental, 20, null);
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    void adminRoutesRequireTheAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardCountsOnlyConfirmedAndCompletedQuantities() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resortAccessReservations").value(2))
                .andExpect(jsonPath("$.liftTicketReservations").value(5))
                .andExpect(jsonPath("$.lessonReservations").value(1))
                .andExpect(jsonPath("$.rentalReservations").value(4));
    }

    @Test
    void reservationListAndDetailIncludeCustomerItemAndPaymentData() throws Exception {
        mockMvc.perform(get("/api/admin/bookings")
                        .param("category", "LESSON")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingNumber").value("SKI-ADMIN-CONFIRMED"))
                .andExpect(jsonPath("$[0].customerEmail").value("customer@example.com"))
                .andExpect(jsonPath("$[0].paymentStatus").value("SUCCEEDED"))
                .andExpect(jsonPath("$[0].item.category").value("LESSON"))
                .andExpect(jsonPath("$[0].item.lessonSessionId").value(reservedSession.getId()));

        mockMvc.perform(get("/api/admin/bookings/{id}", confirmedBooking.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingNumber").value("SKI-ADMIN-CONFIRMED"))
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCEEDED"));
    }

    @Test
    void productManagementCreatesUpdatesAndSoftDeactivates() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Child Lift Pass", "LIFT_TICKET", "79.00", true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        long productId = createdBody.get("id").longValue();

        mockMvc.perform(put("/api/admin/products/{id}", productId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest("Child Lift Pass", "LIFT_TICKET", "85.00", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(85.00));

        mockMvc.perform(delete("/api/admin/products/{id}", productId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        assertThat(productRepository.findById(productId).orElseThrow().isActive()).isFalse();

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/admin/products")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].active".formatted(productId)).value(false));
    }

    @Test
    void lessonSessionManagementProtectsExistingReservations() throws Exception {
        LocalDate newDate = LocalDate.now().plusDays(20);
        MvcResult created = mockMvc.perform(post("/api/admin/lesson-sessions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequest(lesson.getId(), newDate, "09:00", "11:00", 6, "ACTIVE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookedCount").value(0))
                .andExpect(jsonPath("$.availableCount").value(6))
                .andReturn();
        long sessionId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").longValue();

        mockMvc.perform(put("/api/admin/lesson-sessions/{id}", sessionId)
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequest(lesson.getId(), newDate, "10:00", "12:00", 8, "ACTIVE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(8));

        mockMvc.perform(put("/api/admin/lesson-sessions/{id}", reservedSession.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequest(
                                lesson.getId(),
                                reservedSession.getSessionDate(),
                                "09:00",
                                "11:00",
                                4,
                                "CANCELLED")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ADMIN_REQUEST"));

        mockMvc.perform(post("/api/admin/lesson-sessions")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sessionRequest(liftTicket.getId(), newDate.plusDays(1), "09:00", "11:00", 4, "ACTIVE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ADMIN_REQUEST"));
    }

    private String productRequest(String name, String category, String price, boolean active) {
        return """
                {
                  "resortId": %d,
                  "name": "%s",
                  "category": "%s",
                  "description": "Admin managed product",
                  "price": %s,
                  "imageUrl": "https://example.com/product.jpg",
                  "active": %s
                }
                """.formatted(resort.getId(), name, category, price, active);
    }

    private String sessionRequest(
            long productId,
            LocalDate date,
            String start,
            String end,
            int capacity,
            String status) {
        return """
                {
                  "productId": %d,
                  "date": "%s",
                  "startTime": "%s",
                  "endTime": "%s",
                  "capacity": %d,
                  "status": "%s"
                }
                """.formatted(productId, date, start, end, capacity, status);
    }

    private Booking createBooking(String suffix, BookingStatus status) {
        Booking booking = new Booking();
        booking.setBookingNumber("SKI-" + suffix);
        booking.setUser(customer);
        booking.setCustomerFirstName("Customer");
        booking.setCustomerLastName("Example");
        booking.setCustomerEmail(customer.getEmail());
        booking.setCustomerPhone("+61 400 000 000");
        booking.setStatus(status);
        booking.setCurrency("AUD");
        booking.setTotalAmount(new BigDecimal("1000.00"));
        return bookingRepository.saveAndFlush(booking);
    }

    private void addItem(Booking booking, Product product, int quantity, LessonSession session) {
        BookingItem item = new BookingItem();
        item.setBooking(booking);
        item.setProduct(product);
        item.setLessonSession(session);
        item.setProductName(product.getName());
        item.setCategory(product.getCategory());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        if (product.getCategory() == ProductCategory.LIFT_TICKET) {
            item.setBookingDate(LocalDate.now().plusDays(5));
        }
        bookingItemRepository.saveAndFlush(item);
    }

    private void addSucceededPayment(Booking booking) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setStripePaymentId("pi_admin_test");
        payment.setAmount(booking.getTotalAmount());
        payment.setCurrency("AUD");
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setPaymentMethod("card");
        paymentRepository.saveAndFlush(payment);
    }

    private LessonSession createLessonSession(
            Product product,
            int capacity,
            int bookedCount,
            LocalDate date) {
        LessonSession session = new LessonSession();
        session.setProduct(product);
        session.setSessionDate(date);
        session.setStartTime(LocalTime.of(9, 0));
        session.setEndTime(LocalTime.of(11, 0));
        session.setCapacity(capacity);
        session.setBookedCount(bookedCount);
        session.setStatus(LessonSessionStatus.ACTIVE);
        return lessonSessionRepository.saveAndFlush(session);
    }

    private Product createProduct(String name, ProductCategory category, String price) {
        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.saveAndFlush(product);
    }

    private Resort createResort() {
        Resort newResort = new Resort();
        newResort.setName("Snow Alpine Resort");
        newResort.setLocation("Victoria, Australia");
        newResort.setStatus(ResortStatus.ACTIVE);
        return resortRepository.saveAndFlush(newResort);
    }

    private User createUser(String email, UserRole role) {
        User user = new User();
        user.setFirstName(role == UserRole.ADMIN ? "Admin" : "Customer");
        user.setLastName("Example");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(role);
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
