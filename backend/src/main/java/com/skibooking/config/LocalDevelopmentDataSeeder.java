package com.skibooking.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.LessonSession;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;
import com.skibooking.repository.LessonSessionRepository;

@Component
@Profile("local")
public class LocalDevelopmentDataSeeder implements ApplicationRunner {

    private static final String RESORT_NAME = "Snow Alpine Resort";
    private static final String LEGACY_RESORT_NAME = "Snowgum Alpine Resort";

    private final ResortRepository resortRepository;
    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;

    public LocalDevelopmentDataSeeder(
            ResortRepository resortRepository,
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository) {
        this.resortRepository = resortRepository;
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Resort resort = findOrCreateDevelopmentResort();

        createProductIfMissing(
                resort,
                "Daily Vehicle Entry",
                ProductCategory.RESORT_ACCESS,
                "One-day vehicle resort access.",
                "55.00");
        createProductIfMissing(
                resort,
                "Adult Full Day Lift Pass",
                ProductCategory.LIFT_TICKET,
                "Adult full-day lift access.",
                "135.00");
        Product lessonProduct = createProductIfMissing(
                resort,
                "Beginner Ski Lesson",
                ProductCategory.LESSON,
                "Two-hour beginner group ski lesson.",
                "120.00");
        createProductIfMissing(
                resort,
                "Ski Package",
                ProductCategory.RENTAL,
                "Skis, boots, and poles.",
                "65.00");

        createLessonSessionIfMissing(
                lessonProduct,
                LocalDate.of(2026, 8, 25),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                8);
        createLessonSessionIfMissing(
                lessonProduct,
                LocalDate.of(2026, 8, 25),
                LocalTime.of(13, 0),
                LocalTime.of(15, 0),
                8);
        createLessonSessionIfMissing(
                lessonProduct,
                LocalDate.of(2026, 8, 26),
                LocalTime.of(9, 0),
                LocalTime.of(11, 0),
                8);
    }

    private Resort findOrCreateDevelopmentResort() {
        return resortRepository.findByName(RESORT_NAME)
                .orElseGet(() -> resortRepository.findByName(LEGACY_RESORT_NAME)
                        .map(this::renameLegacyDevelopmentResort)
                        .orElseGet(this::createDevelopmentResort));
    }

    private Resort renameLegacyDevelopmentResort(Resort resort) {
        resort.setName(RESORT_NAME);
        return resortRepository.save(resort);
    }

    private Resort createDevelopmentResort() {
        Resort resort = new Resort();
        resort.setName(RESORT_NAME);
        resort.setLocation("Victoria, Australia");
        resort.setDescription("Fictional resort used only for local development.");
        resort.setStatus(ResortStatus.ACTIVE);
        return resortRepository.save(resort);
    }

    private Product createProductIfMissing(
            Resort resort,
            String name,
            ProductCategory category,
            String description,
            String price) {
        return productRepository.findByResortIdAndName(resort.getId(), name)
                .orElseGet(() -> createProduct(resort, name, category, description, price));
    }

    private Product createProduct(
            Resort resort,
            String name,
            ProductCategory category,
            String description,
            String price) {
        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.save(product);
    }

    private void createLessonSessionIfMissing(
            Product product,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int capacity) {
        if (lessonSessionRepository.existsByProductIdAndSessionDateAndStartTime(
                product.getId(), date, startTime)) {
            return;
        }

        LessonSession session = new LessonSession();
        session.setProduct(product);
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCapacity(capacity);
        session.setBookedCount(0);
        session.setStatus(LessonSessionStatus.ACTIVE);
        lessonSessionRepository.save(session);
    }
}
