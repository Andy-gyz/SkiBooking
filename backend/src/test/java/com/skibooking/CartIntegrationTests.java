package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.CartItemRepository;
import com.skibooking.repository.CartRepository;
import com.skibooking.repository.EmailVerificationCodeRepository;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;
import com.skibooking.repository.UserRepository;
import com.skibooking.service.VerificationCodeGenerator;
import com.skibooking.service.VerificationEmailSender;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CartIntegrationTests {

    private static final String CART_TOKEN_HEADER = "X-Cart-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private EmailVerificationCodeRepository verificationCodeRepository;

    @MockitoBean
    private VerificationCodeGenerator codeGenerator;

    @MockitoBean
    private VerificationEmailSender emailSender;

    private Product resortAccessProduct;
    private Product liftProduct;
    private Product lessonProduct;
    private Product rentalProduct;
    private LessonSession lessonSession;

    @BeforeEach
    void setUp() {
        when(codeGenerator.generate()).thenReturn("123456");
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        lessonSessionRepository.deleteAll();
        productRepository.deleteAll();
        resortRepository.deleteAll();
        userRepository.deleteAll();
        verificationCodeRepository.deleteAll();

        Resort resort = new Resort();
        resort.setName("Snow Alpine Resort");
        resort.setLocation("Victoria, Australia");
        resort.setStatus(ResortStatus.ACTIVE);
        resortRepository.saveAndFlush(resort);

        resortAccessProduct = createProduct(
                resort, "Daily Vehicle Entry", ProductCategory.RESORT_ACCESS, "55.00");
        liftProduct = createProduct(
                resort, "Adult Full Day Lift Pass", ProductCategory.LIFT_TICKET, "135.00");
        lessonProduct = createProduct(
                resort, "Beginner Ski Lesson", ProductCategory.LESSON, "120.00");
        rentalProduct = createProduct(
                resort, "Ski Package", ProductCategory.RENTAL, "65.00");

        lessonSession = new LessonSession();
        lessonSession.setProduct(lessonProduct);
        lessonSession.setSessionDate(LocalDate.of(2026, 8, 25));
        lessonSession.setStartTime(LocalTime.of(9, 0));
        lessonSession.setEndTime(LocalTime.of(11, 0));
        lessonSession.setCapacity(8);
        lessonSession.setBookedCount(0);
        lessonSession.setStatus(LessonSessionStatus.ACTIVE);
        lessonSessionRepository.saveAndFlush(lessonSession);
    }

    @Test
    void anonymousCartRequiresAnUnpredictableToken() throws Exception {
        CreatedCart cart = createAnonymousCart();

        assertThat(cart.token()).hasSize(43);
        mockMvc.perform(get("/api/carts/{cartId}", cart.id()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CART_NOT_FOUND"));
        mockMvc.perform(get("/api/carts/{cartId}", cart.id())
                        .header(CART_TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/carts/{cartId}", cart.id())
                        .header(CART_TOKEN_HEADER, cart.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void oneAnonymousCartAcceptsAllFourProductCategoriesAndCalculatesTrustedTotal() throws Exception {
        CreatedCart cart = createAnonymousCart();

        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 1,
                  "vehicleRegistration": "ABC123",
                  "vehicleType": "SUV",
                  "entryDate": "2026-08-25",
                  "exitDate": "2026-08-26"
                }
                """.formatted(resortAccessProduct.getId())).andExpect(status().isCreated());
        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 2,
                  "bookingDate": "2026-08-25"
                }
                """.formatted(liftProduct.getId())).andExpect(status().isCreated());
        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 1,
                  "lessonSessionId": %d
                }
                """.formatted(lessonProduct.getId(), lessonSession.getId()))
                .andExpect(status().isCreated());
        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 1,
                  "rentalStartDate": "2026-08-25",
                  "rentalEndDate": "2026-08-26",
                  "rentalSize": "Adult Medium",
                  "rentalBootSize": "AU 9"
                }
                """.formatted(rentalProduct.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()").value(4))
                .andExpect(jsonPath("$.itemCount").value(5))
                .andExpect(jsonPath("$.subtotal").value(510.00))
                .andExpect(jsonPath("$.total").value(510.00))
                .andExpect(jsonPath("$.currency").value("AUD"));
    }

    @Test
    void cartItemsCanBeUpdatedAndDeleted() throws Exception {
        CreatedCart cart = createAnonymousCart();
        MvcResult addResult = addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 2,
                  "bookingDate": "2026-08-25"
                }
                """.formatted(liftProduct.getId()))
                .andExpect(status().isCreated())
                .andReturn();
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .get("items").get(0).get("id").longValue();

        mockMvc.perform(put("/api/carts/{cartId}/items/{itemId}", cart.id(), itemId)
                        .header(CART_TOKEN_HEADER, cart.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 3,
                                  "bookingDate": "2026-08-26"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].bookingDate").value("2026-08-26"))
                .andExpect(jsonPath("$.total").value(405.00));

        mockMvc.perform(delete("/api/carts/{cartId}/items/{itemId}", cart.id(), itemId)
                        .header(CART_TOKEN_HEADER, cart.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/carts/{cartId}", cart.id())
                        .header(CART_TOKEN_HEADER, cart.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void categoryRulesAndLessonCapacityAreEnforced() throws Exception {
        CreatedCart cart = createAnonymousCart();

        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 1
                }
                """.formatted(resortAccessProduct.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CART_ITEM"));

        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 1,
                  "bookingDate": "2026-08-25",
                  "vehicleType": "unexpected"
                }
                """.formatted(liftProduct.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CART_ITEM"));

        addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 9,
                  "lessonSessionId": %d
                }
                """.formatted(lessonProduct.getId(), lessonSession.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_LESSON_CAPACITY"));
    }

    @Test
    void registrationClaimsAnonymousCartAndInvalidatesItsToken() throws Exception {
        CreatedCart anonymousCart = createAnonymousCart();
        addLiftTicket(anonymousCart).andExpect(status().isCreated());

        MvcResult registration = register("new@example.com", anonymousCart.token())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cartId").value(anonymousCart.id()))
                .andReturn();
        String accessToken = objectMapper.readTree(registration.getResponse().getContentAsString())
                .get("accessToken").stringValue();

        mockMvc.perform(get("/api/carts/{cartId}", anonymousCart.id())
                        .header(CART_TOKEN_HEADER, anonymousCart.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/carts/{cartId}", anonymousCart.id())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void loginMergesAnonymousItemsIntoExistingUserCart() throws Exception {
        MvcResult registration = register("existing@example.com", null)
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = objectMapper.readTree(registration.getResponse().getContentAsString())
                .get("accessToken").stringValue();

        MvcResult userCartResult = mockMvc.perform(post("/api/carts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andReturn();
        long userCartId = objectMapper.readTree(userCartResult.getResponse().getContentAsString())
                .get("cart").get("id").longValue();
        mockMvc.perform(post("/api/carts/{cartId}/items", userCartId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 1,
                                  "rentalStartDate": "2026-08-25",
                                  "rentalEndDate": "2026-08-26",
                                  "rentalSize": "Adult Medium"
                                }
                                """.formatted(rentalProduct.getId())))
                .andExpect(status().isCreated());

        CreatedCart anonymousCart = createAnonymousCart();
        addLiftTicket(anonymousCart).andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "existing@example.com",
                                  "password": "Secret123!",
                                  "cartToken": "%s"
                                }
                                """.formatted(anonymousCart.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(userCartId))
                .andReturn();
        String mergedAccessToken = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("accessToken").stringValue();

        mockMvc.perform(get("/api/carts/{cartId}", userCartId)
                        .header("Authorization", "Bearer " + mergedAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(335.00));
        mockMvc.perform(get("/api/carts/{cartId}", anonymousCart.id())
                        .header(CART_TOKEN_HEADER, anonymousCart.token()))
                .andExpect(status().isNotFound());
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

    private CreatedCart createAnonymousCart() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/carts"))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return new CreatedCart(
                response.get("cart").get("id").longValue(),
                response.get("cartToken").stringValue());
    }

    private org.springframework.test.web.servlet.ResultActions addItem(CreatedCart cart, String request)
            throws Exception {
        return mockMvc.perform(post("/api/carts/{cartId}/items", cart.id())
                .header(CART_TOKEN_HEADER, cart.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(request));
    }

    private org.springframework.test.web.servlet.ResultActions addLiftTicket(CreatedCart cart)
            throws Exception {
        return addItem(cart, """
                {
                  "productId": %d,
                  "quantity": 2,
                  "bookingDate": "2026-08-25"
                }
                """.formatted(liftProduct.getId()));
    }

    private org.springframework.test.web.servlet.ResultActions register(String email, String cartToken)
            throws Exception {
        mockMvc.perform(post("/api/auth/verification-codes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "%s" }
                                """.formatted(email)))
                .andExpect(status().isOk());
        String cartTokenJson = cartToken == null ? "null" : "\"" + cartToken + "\"";
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Test",
                          "lastName": "Customer",
                          "email": "%s",
                          "password": "Secret123!",
                          "verificationCode": "123456",
                          "cartToken": %s
                        }
                        """.formatted(email, cartTokenJson)));
    }

    private record CreatedCart(long id, String token) {
    }
}
