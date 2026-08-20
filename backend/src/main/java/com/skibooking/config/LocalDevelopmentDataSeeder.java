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
                "55.00",
                "/images/resort-entry-car.jpg");
        createProductIfMissing(
                resort,
                "Two-Day Vehicle Entry",
                ProductCategory.RESORT_ACCESS,
                "Vehicle access for two consecutive snow days.",
                "95.00",
                "/images/entry-two-day.jpg");
        createProductIfMissing(
                resort,
                "Oversize Vehicle Entry",
                ProductCategory.RESORT_ACCESS,
                "Daily entry for campervans, minibuses, and oversized vehicles.",
                "85.00",
                "/images/entry-oversize.jpg");
        createProductIfMissing(
                resort,
                "Season Parking Pass",
                ProductCategory.RESORT_ACCESS,
                "Reusable vehicle access throughout the 2026 snow season.",
                "420.00",
                "/images/entry-season.jpg");
        createProductIfMissing(
                resort,
                "Adult Full Day Lift Pass",
                ProductCategory.LIFT_TICKET,
                "Adult full-day lift access.",
                "135.00",
                "/images/lift-pass.jpg");
        createProductIfMissing(
                resort,
                "Child Full Day Lift Pass",
                ProductCategory.LIFT_TICKET,
                "Full-day lift access for guests aged 5 to 17.",
                "75.00",
                "/images/lift-child.jpg");
        createProductIfMissing(
                resort,
                "Afternoon Lift Pass",
                ProductCategory.LIFT_TICKET,
                "Lift access from 12:30 pm until close.",
                "95.00",
                "/images/lift-afternoon.jpg");
        createProductIfMissing(
                resort,
                "Beginner Area Lift Pass",
                ProductCategory.LIFT_TICKET,
                "All-day access to beginner lifts and learning terrain.",
                "68.00",
                "/images/lift-beginner.jpg");
        Product lessonProduct = createProductIfMissing(
                resort,
                "Beginner Ski Lesson",
                ProductCategory.LESSON,
                "Two-hour beginner group ski lesson.",
                "120.00",
                "/images/ski-lesson.jpg");
        Product snowboardLesson = createProductIfMissing(
                resort,
                "Beginner Snowboard Lesson",
                ProductCategory.LESSON,
                "Two-hour beginner group snowboard lesson.",
                "125.00",
                "/images/lesson-snowboard.jpg");
        Product kidsLesson = createProductIfMissing(
                resort,
                "Kids Snow Club",
                ProductCategory.LESSON,
                "A playful two-hour ski program for children aged 6 to 12.",
                "110.00",
                "/images/lesson-kids.jpg");
        Product privateLesson = createProductIfMissing(
                resort,
                "Private Ski Coaching",
                ProductCategory.LESSON,
                "A focused two-hour private session tailored to your goals.",
                "240.00",
                "/images/lesson-private.jpg");
        createProductIfMissing(
                resort,
                "Ski Package",
                ProductCategory.RENTAL,
                "Skis, boots, and poles.",
                "65.00",
                "/images/equipment-rental.jpg");
        createProductIfMissing(
                resort,
                "Snowboard Package",
                ProductCategory.RENTAL,
                "Snowboard, boots, and wrist guards.",
                "70.00",
                "/images/rental-snowboard.jpg");
        createProductIfMissing(
                resort,
                "Performance Ski Package",
                ProductCategory.RENTAL,
                "Premium skis, boots, and poles for confident riders.",
                "95.00",
                "/images/rental-performance.jpg");
        createProductIfMissing(
                resort,
                "Jacket and Pants Package",
                ProductCategory.RENTAL,
                "Waterproof outerwear for one snow day.",
                "45.00",
                "/images/rental-clothing.jpg");

        LocalDate firstDay = LocalDate.now().plusDays(1);
        for (int offset = 0; offset < 7; offset++) {
            LocalDate sessionDate = firstDay.plusDays(offset);
            createLessonSessionIfMissing(lessonProduct, sessionDate, LocalTime.of(9, 0), LocalTime.of(11, 0), 8);
            createLessonSessionIfMissing(lessonProduct, sessionDate, LocalTime.of(13, 0), LocalTime.of(15, 0), 8);
            createLessonSessionIfMissing(snowboardLesson, sessionDate, LocalTime.of(10, 0), LocalTime.of(12, 0), 8);
            createLessonSessionIfMissing(snowboardLesson, sessionDate, LocalTime.of(14, 0), LocalTime.of(16, 0), 8);
            createLessonSessionIfMissing(kidsLesson, sessionDate, LocalTime.of(9, 30), LocalTime.of(11, 30), 10);
            createLessonSessionIfMissing(privateLesson, sessionDate, LocalTime.of(11, 30), LocalTime.of(13, 30), 4);
        }
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
            String price,
            String imageUrl) {
        return productRepository.findByResortIdAndName(resort.getId(), name)
                .map(product -> addImageIfMissing(product, imageUrl))
                .orElseGet(() -> createProduct(resort, name, category, description, price, imageUrl));
    }

    private Product addImageIfMissing(Product product, String imageUrl) {
        if (product.getImageUrl() == null || product.getImageUrl().isBlank()) {
            product.setImageUrl(imageUrl);
            return productRepository.save(product);
        }
        return product;
    }

    private Product createProduct(
            Resort resort,
            String name,
            ProductCategory category,
            String description,
            String price,
            String imageUrl) {
        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setImageUrl(imageUrl);
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
