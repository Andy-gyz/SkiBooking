package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.skibooking.entity.Cart;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.User;
import com.skibooking.entity.enums.CartStatus;
import com.skibooking.entity.enums.LessonSessionStatus;
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
class BookingIntegrationTests {

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

    private LocalDate bookingDate;
    private Product liftProduct;
    private Product lessonProduct;
    private LessonSession lessonSession;

    @BeforeEach
    void setUp() {
        clearDatabase();
        bookingDate = LocalDate.now().plusDays(5);

        Resort resort = new Resort();
        resort.setName("Snow Alpine Resort");
        resort.setLocation("Victoria, Australia");
        resort.setStatus(ResortStatus.ACTIVE);
        resortRepository.saveAndFlush(resort);

        liftProduct = createProduct(
                resort, "Adult Full Day Lift Pass", ProductCategory.LIFT_TICKET, "135.00");
        lessonProduct = createProduct(
                resort, "Beginner Ski Lesson", ProductCategory.LESSON, "120.00");

        lessonSession = new LessonSession();
        lessonSession.setProduct(lessonProduct);
        lessonSession.setSessionDate(bookingDate);
        lessonSession.setStartTime(LocalTime.of(9, 0));
        lessonSession.setEndTime(LocalTime.of(11, 0));
        lessonSession.setCapacity(2);
        lessonSession.setBookedCount(0);
        lessonSession.setStatus(LessonSessionStatus.ACTIVE);
        lessonSessionRepository.saveAndFlush(lessonSession);
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    void checkoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest(1L)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void checkoutRepricesCartAndPreservesBookingSnapshot() throws Exception {
        AuthenticatedUser customer = createUser("customer@example.com");
        long cartId = createAuthenticatedCart(customer.token());
        addLiftTicket(customer.token(), cartId, 2).andExpect(status().isCreated());

        liftProduct.setPrice(new BigDecimal("150.00"));
        productRepository.saveAndFlush(liftProduct);

        MvcResult checkout = checkout(customer.token(), cartId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.currency").value("AUD"))
                .andExpect(jsonPath("$.totalAmount").value(300.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(150.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(300.00))
                .andExpect(jsonPath("$.customerEmail").value("billing@example.com"))
                .andReturn();
        String bookingNumber = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .get("bookingNumber").stringValue();
        assertThat(bookingNumber).matches("SKI-\\d{8}-[A-F0-9]{12}");

        Cart checkedOutCart = cartRepository.findById(cartId).orElseThrow();
        assertThat(checkedOutCart.getStatus()).isEqualTo(CartStatus.CHECKED_OUT);

        liftProduct.setName("Renamed Lift Pass");
        liftProduct.setPrice(new BigDecimal("999.00"));
        productRepository.saveAndFlush(liftProduct);

        mockMvc.perform(get("/api/bookings/{bookingNumber}", bookingNumber)
                        .header("Authorization", bearer(customer.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productName").value("Adult Full Day Lift Pass"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(150.00))
                .andExpect(jsonPath("$.totalAmount").value(300.00));

        checkout(customer.token(), cartId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
    }

    @Test
    void bookingHistoryAndDetailEnforceOwnership() throws Exception {
        AuthenticatedUser owner = createUser("owner@example.com");
        AuthenticatedUser otherUser = createUser("other@example.com");
        long cartId = createAuthenticatedCart(owner.token());
        addLiftTicket(owner.token(), cartId, 2).andExpect(status().isCreated());
        MvcResult checkout = checkout(owner.token(), cartId)
                .andExpect(status().isCreated())
                .andReturn();
        String bookingNumber = objectMapper.readTree(checkout.getResponse().getContentAsString())
                .get("bookingNumber").stringValue();

        mockMvc.perform(get("/api/my-bookings")
                        .header("Authorization", bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].bookingNumber").value(bookingNumber))
                .andExpect(jsonPath("$[0].itemCount").value(2));

        mockMvc.perform(get("/api/bookings/{bookingNumber}", bookingNumber)
                        .header("Authorization", bearer(otherUser.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOKING_NOT_FOUND"));
    }

    @Test
    void lessonCapacityLockPreventsSecondCartFromOverbookingAndRollsBack() throws Exception {
        AuthenticatedUser first = createUser("first@example.com");
        AuthenticatedUser second = createUser("second@example.com");
        long firstCartId = createAuthenticatedCart(first.token());
        long secondCartId = createAuthenticatedCart(second.token());
        addLesson(first.token(), firstCartId, 2).andExpect(status().isCreated());
        addLesson(second.token(), secondCartId, 2).andExpect(status().isCreated());

        checkout(first.token(), firstCartId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].lessonSessionId").value(lessonSession.getId()));
        checkout(second.token(), secondCartId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_LESSON_CAPACITY"));

        LessonSession updatedSession = lessonSessionRepository.findById(lessonSession.getId()).orElseThrow();
        assertThat(updatedSession.getBookedCount()).isEqualTo(2);
        assertThat(bookingRepository.count()).isEqualTo(1);
        assertThat(cartRepository.findById(secondCartId).orElseThrow().getStatus())
                .isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void unavailableProductRejectsCheckoutWithoutChangingTheCart() throws Exception {
        AuthenticatedUser customer = createUser("customer@example.com");
        long cartId = createAuthenticatedCart(customer.token());
        addLiftTicket(customer.token(), cartId, 1).andExpect(status().isCreated());
        liftProduct.setActive(false);
        productRepository.saveAndFlush(liftProduct);

        checkout(customer.token(), cartId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CHECKOUT"));

        assertThat(bookingRepository.count()).isZero();
        assertThat(cartRepository.findById(cartId).orElseThrow().getStatus())
                .isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void emptyCartCannotBeCheckedOut() throws Exception {
        AuthenticatedUser customer = createUser("customer@example.com");
        long cartId = createAuthenticatedCart(customer.token());

        checkout(customer.token(), cartId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EMPTY_CART"));
    }

    private Product createProduct(
            Resort resort,
            String name,
            ProductCategory category,
            String price) {
        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.saveAndFlush(product);
    }

    private AuthenticatedUser createUser(String email) {
        User user = new User();
        user.setFirstName("Test");
        user.setLastName("Customer");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("Secret123!"));
        user.setRole(UserRole.CUSTOMER);
        userRepository.saveAndFlush(user);
        return new AuthenticatedUser(user, jwtService.createAuthResponse(user, null).accessToken());
    }

    private long createAuthenticatedCart(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/carts")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("cart").get("id").longValue();
    }

    private org.springframework.test.web.servlet.ResultActions addLiftTicket(
            String token,
            long cartId,
            int quantity) throws Exception {
        return mockMvc.perform(post("/api/carts/{cartId}/items", cartId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "productId": %d,
                          "quantity": %d,
                          "bookingDate": "%s"
                        }
                        """.formatted(liftProduct.getId(), quantity, bookingDate)));
    }

    private org.springframework.test.web.servlet.ResultActions addLesson(
            String token,
            long cartId,
            int quantity) throws Exception {
        return mockMvc.perform(post("/api/carts/{cartId}/items", cartId)
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "productId": %d,
                          "quantity": %d,
                          "lessonSessionId": %d
                        }
                        """.formatted(lessonProduct.getId(), quantity, lessonSession.getId())));
    }

    private org.springframework.test.web.servlet.ResultActions checkout(String token, long cartId)
            throws Exception {
        return mockMvc.perform(post("/api/bookings")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutRequest(cartId)));
    }

    private String checkoutRequest(long cartId) {
        return """
                {
                  "cartId": %d,
                  "firstName": "Billing",
                  "lastName": "Customer",
                  "email": "billing@example.com",
                  "phone": "+61 400 000 000"
                }
                """.formatted(cartId);
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

    private record AuthenticatedUser(User user, String token) {
    }
}
