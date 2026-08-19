package com.skibooking.config;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.Product;
import com.skibooking.entity.Resort;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.ProductRepository;
import com.skibooking.repository.ResortRepository;

@Component
@Profile("local")
public class LocalDevelopmentDataSeeder implements ApplicationRunner {

    private static final String RESORT_NAME = "Snowgum Alpine Resort";

    private final ResortRepository resortRepository;
    private final ProductRepository productRepository;

    public LocalDevelopmentDataSeeder(
            ResortRepository resortRepository,
            ProductRepository productRepository) {
        this.resortRepository = resortRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Resort resort = resortRepository.findByName(RESORT_NAME)
                .orElseGet(this::createDevelopmentResort);

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
        createProductIfMissing(
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
    }

    private Resort createDevelopmentResort() {
        Resort resort = new Resort();
        resort.setName(RESORT_NAME);
        resort.setLocation("Victoria, Australia");
        resort.setDescription("Fictional resort used only for local development.");
        resort.setStatus(ResortStatus.ACTIVE);
        return resortRepository.save(resort);
    }

    private void createProductIfMissing(
            Resort resort,
            String name,
            ProductCategory category,
            String description,
            String price) {
        if (productRepository.existsByResortIdAndName(resort.getId(), name)) {
            return;
        }

        Product product = new Product();
        product.setResort(resort);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        productRepository.save(product);
    }
}

