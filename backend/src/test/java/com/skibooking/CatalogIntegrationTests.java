package com.skibooking;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogIntegrationTests {

    private static final LocalDate LESSON_DATE = LocalDate.of(2026, 8, 25);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResortRepository resortRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private LessonSessionRepository lessonSessionRepository;

    private Resort activeResort;
    private Resort inactiveResort;
    private Product liftProduct;
    private Product lessonProduct;
    private Product inactiveProduct;

    @BeforeEach
    void setUpCatalog() {
        lessonSessionRepository.deleteAll();
        productRepository.deleteAll();
        resortRepository.deleteAll();

        activeResort = createResort("Snow Alpine Resort", ResortStatus.ACTIVE);
        inactiveResort = createResort("Closed Mountain", ResortStatus.INACTIVE);
        liftProduct = createProduct(activeResort, "Adult Lift Pass", ProductCategory.LIFT_TICKET, true);
        lessonProduct = createProduct(activeResort, "Beginner Lesson", ProductCategory.LESSON, true);
        inactiveProduct = createProduct(activeResort, "Retired Pass", ProductCategory.LIFT_TICKET, false);
        createProduct(inactiveResort, "Hidden Resort Pass", ProductCategory.LIFT_TICKET, true);

        createSession(LocalTime.of(13, 0), LessonSessionStatus.ACTIVE, 8, 0);
        createSession(LocalTime.of(9, 0), LessonSessionStatus.ACTIVE, 8, 3);
        createSession(LocalTime.of(15, 30), LessonSessionStatus.CANCELLED, 8, 0);
    }

    @Test
    void resortsArePublicAndOnlyExposeActiveLocations() throws Exception {
        mockMvc.perform(get("/api/resorts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Snow Alpine Resort"));

        mockMvc.perform(get("/api/resorts/{id}", activeResort.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Victoria, Australia"));

        mockMvc.perform(get("/api/resorts/{id}", inactiveResort.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void productsArePublicFilterableAndExcludeUnavailableInventory() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/products").queryParam("category", "LIFT_TICKET"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(liftProduct.getId()))
                .andExpect(jsonPath("$[0].resort.name").value("Snow Alpine Resort"))
                .andExpect(jsonPath("$[0].currency").value("AUD"));
    }

    @Test
    void productDetailReturnsOnlyAvailableProducts() throws Exception {
        mockMvc.perform(get("/api/products/{id}", lessonProduct.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("LESSON"))
                .andExpect(jsonPath("$.price").value(120.00));

        mockMvc.perform(get("/api/products/{id}", inactiveProduct.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void lessonSessionsReturnActiveAvailabilityInStartTimeOrder() throws Exception {
        mockMvc.perform(get("/api/lesson-sessions")
                        .queryParam("productId", lessonProduct.getId().toString())
                        .queryParam("date", LESSON_DATE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].availableCount").value(5))
                .andExpect(jsonPath("$[1].startTime").value("13:00:00"))
                .andExpect(jsonPath("$[1].availableCount").value(8));
    }

    @Test
    void lessonSessionQueryRejectsNonLessonProducts() throws Exception {
        mockMvc.perform(get("/api/lesson-sessions")
                        .queryParam("productId", liftProduct.getId().toString())
                        .queryParam("date", LESSON_DATE.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CATALOG_REQUEST"));
    }

    @Test
    void invalidCatalogParametersUseTheStandardErrorContract() throws Exception {
        mockMvc.perform(get("/api/products").queryParam("category", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get("/api/lesson-sessions")
                        .queryParam("productId", lessonProduct.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }

    private Resort createResort(String name, ResortStatus status) {
        Resort resort = new Resort();
        resort.setName(name);
        resort.setLocation("Victoria, Australia");
        resort.setDescription("Test resort");
        resort.setStatus(status);
        return resortRepository.saveAndFlush(resort);
    }

    private Product createProduct(
            Resort resort,
            String name,
            ProductCategory category,
            boolean active) {
        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setDescription("Test product");
        product.setPrice(category == ProductCategory.LESSON
                ? new BigDecimal("120.00")
                : new BigDecimal("135.00"));
        product.setActive(active);
        return productRepository.saveAndFlush(product);
    }

    private void createSession(
            LocalTime startTime,
            LessonSessionStatus status,
            int capacity,
            int bookedCount) {
        LessonSession session = new LessonSession();
        session.setProduct(lessonProduct);
        session.setSessionDate(LESSON_DATE);
        session.setStartTime(startTime);
        session.setEndTime(startTime.plusHours(2));
        session.setCapacity(capacity);
        session.setBookedCount(bookedCount);
        session.setStatus(status);
        lessonSessionRepository.saveAndFlush(session);
    }
}
