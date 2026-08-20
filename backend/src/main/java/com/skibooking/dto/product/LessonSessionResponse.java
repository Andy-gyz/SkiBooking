package com.skibooking.dto.product;

import java.time.LocalDate;
import java.time.LocalTime;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.enums.LessonSessionStatus;

public record LessonSessionResponse(
        Long id,
        Long productId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int availableCount,
        LessonSessionStatus status) {

    public static LessonSessionResponse from(LessonSession session) {
        return new LessonSessionResponse(
                session.getId(),
                session.getProduct().getId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getAvailableCount(),
                session.getStatus());
    }
}
