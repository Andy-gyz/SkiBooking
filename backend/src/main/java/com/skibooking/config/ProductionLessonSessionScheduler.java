package com.skibooking.config;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.skibooking.service.LessonSessionGenerationService;

@Component
@Profile("production")
public class ProductionLessonSessionScheduler implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductionLessonSessionScheduler.class);
    private static final ZoneId RESORT_TIME_ZONE = ZoneId.of("Australia/Melbourne");

    private final LessonSessionGenerationService generationService;

    public ProductionLessonSessionScheduler(LessonSessionGenerationService generationService) {
        this.generationService = generationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        generateUpcomingSessions();
    }

    @Scheduled(
            cron = "${app.lesson-sessions.generation-cron:0 15 2 * * *}",
            zone = "Australia/Melbourne")
    public void generateUpcomingSessions() {
        LocalDate firstDate = LocalDate.now(RESORT_TIME_ZONE).plusDays(1);
        int created = generationService.generateRollingWindow(firstDate);
        LOGGER.info("Generated {} lesson sessions for the rolling production window.", created);
    }
}
