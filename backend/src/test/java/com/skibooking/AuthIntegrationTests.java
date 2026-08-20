package com.skibooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.skibooking.entity.User;
import com.skibooking.repository.CartItemRepository;
import com.skibooking.repository.CartRepository;
import com.skibooking.repository.EmailVerificationCodeRepository;
import com.skibooking.repository.UserRepository;
import com.skibooking.service.VerificationCodeGenerator;
import com.skibooking.service.VerificationEmailSender;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EmailVerificationCodeRepository verificationCodeRepository;

    @MockitoBean
    private VerificationCodeGenerator codeGenerator;

    @MockitoBean
    private VerificationEmailSender emailSender;

    @BeforeEach
    void deleteUsers() {
        when(codeGenerator.generate()).thenReturn("123456");
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
        verificationCodeRepository.deleteAll();
    }

    @Test
    void registerReturnsJwtAndAuthenticatedUserWithoutExposingPassword() throws Exception {
        String responseBody = register("customer@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("customer@example.com"))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        String token = response.get("accessToken").stringValue();

        User storedUser = userRepository.findByEmailIgnoreCase("customer@example.com").orElseThrow();
        assertThat(storedUser.getPasswordHash())
                .isNotEqualTo("Secret123!")
                .startsWith("$2");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(storedUser.getId()))
                .andExpect(jsonPath("$.firstName").value("Test"));

        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void loginAcceptsCaseInsensitiveEmailAndReturnsUsableJwt() throws Exception {
        register("customer@example.com").andExpect(status().isCreated());

        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "CUSTOMER@EXAMPLE.COM",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("customer@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseBody).get("accessToken").stringValue();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateEmailReturnsConflict() throws Exception {
        register("customer@example.com").andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "Customer",
                                  "email": "CUSTOMER@example.com",
                                  "password": "Secret123!",
                                  "verificationCode": "123456"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void invalidRegistrationReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "Customer",
                                  "email": "not-an-email",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors.length()").value(4));
    }

    @Test
    void verificationCodeIsSentHashedAndRequiredForRegistration() throws Exception {
        requestCode("verify@example.com")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresInSeconds").value(600))
                .andExpect(jsonPath("$.resendAfterSeconds").value(60));

        verify(emailSender).sendVerificationCode("verify@example.com", "123456");
        assertThat(verificationCodeRepository.findByEmail("verify@example.com").orElseThrow().getCodeHash())
                .isNotEqualTo("123456")
                .startsWith("$2");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Test",
                                  "lastName": "Customer",
                                  "email": "verify@example.com",
                                  "password": "Secret123!",
                                  "verificationCode": "999999"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_CODE"));

        registerWithoutRequest("verify@example.com")
                .andExpect(status().isCreated());
        assertThat(verificationCodeRepository.findByEmail("verify@example.com")).isEmpty();
    }

    @Test
    void requestingAnotherCodeDuringCooldownIsRateLimited() throws Exception {
        requestCode("cooldown@example.com").andExpect(status().isOk());
        requestCode("cooldown@example.com")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("VERIFICATION_CODE_COOLDOWN"));
    }

    @Test
    void invalidCredentialsReturnGenericUnauthorizedError() throws Exception {
        register("customer@example.com").andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    void protectedRoutesEnforceAuthenticationAndAdminRole() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        String responseBody = register("customer@example.com")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = objectMapper.readTree(responseBody).get("accessToken").stringValue();

        mockMvc.perform(get("/api/admin/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String email) throws Exception {
        requestCode(email).andExpect(status().isOk());
        return registerWithoutRequest(email);
    }

    private org.springframework.test.web.servlet.ResultActions requestCode(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/verification-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s" }
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions registerWithoutRequest(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "firstName": "Test",
                          "lastName": "Customer",
                          "email": "%s",
                          "password": "Secret123!",
                          "verificationCode": "123456",
                          "phone": "+61 400 000 000"
                        }
                        """.formatted(email)));
    }
}
