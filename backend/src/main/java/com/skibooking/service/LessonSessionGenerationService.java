package com.skibooking.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.Product;
import com.skibooking.entity.enums.LessonSessionStatus;
import com.skibooking.entity.enums.ProductCategory;
import com.skibooking.entity.enums.ResortStatus;
import com.skibooking.repository.LessonSessionRepository;
import com.skibooking.repository.ProductRepository;

@Service
public class LessonSessionGenerationService {

    private static final int ROLLING_WINDOW_DAYS = 14;

    private final ProductRepository productRepository;
    private final LessonSessionRepository lessonSessionRepository;

    public LessonSessionGenerationService(
            ProductRepository productRepository,
            LessonSessionRepository lessonSessionRepository) {
        this.productRepository = productRepository;
        this.lessonSessionRepository = lessonSessionRepository;
    }

    @Transactional
    public int generateRollingWindow(LocalDate firstDate) {
        List<Product> products = productRepository
                .findByCategoryAndActiveTrueAndResort_StatusOrderByNameAsc(
                        ProductCategory.LESSON, ResortStatus.ACTIVE);
        int created = 0;
        for (Product product : products) {
            for (int offset = 0; offset < ROLLING_WINDOW_DAYS; offset++) {
                LocalDate date = firstDate.plusDays(offset);
                for (SessionTemplate template : templatesFor(product)) {
                    if (lessonSessionRepository.existsByProductIdAndSessionDateAndStartTime(
                            product.getId(), date, template.startTime())) {
                        continue;
                    }
                    LessonSession session = new LessonSession();
                    session.setProduct(product);
                    session.setSessionDate(date);
                    session.setStartTime(template.startTime());
                    session.setEndTime(template.endTime());
                    session.setCapacity(template.capacity());
                    session.setBookedCount(0);
                    session.setStatus(LessonSessionStatus.ACTIVE);
                    lessonSessionRepository.save(session);
                    created++;
                }
            }
        }
        return created;
    }

    private List<SessionTemplate> templatesFor(Product product) {
        return switch (product.getName()) {
            case "Beginner Ski Lesson" -> List.of(
                    template(9, 0, 11, 0, 8),
                    template(13, 0, 15, 0, 8));
            case "Beginner Snowboard Lesson" -> List.of(
                    template(10, 0, 12, 0, 8),
                    template(14, 0, 16, 0, 8));
            case "Kids Snow Club" -> List.of(template(9, 30, 11, 30, 10));
            case "Private Ski Coaching" -> List.of(template(11, 30, 13, 30, 4));
            default -> List.of(
                    template(9, 0, 11, 0, 8),
                    template(13, 0, 15, 0, 8));
        };
    }

    private SessionTemplate template(
            int startHour,
            int startMinute,
            int endHour,
            int endMinute,
            int capacity) {
        return new SessionTemplate(
                LocalTime.of(startHour, startMinute),
                LocalTime.of(endHour, endMinute),
                capacity);
    }

    private record SessionTemplate(LocalTime startTime, LocalTime endTime, int capacity) {
    }
}
