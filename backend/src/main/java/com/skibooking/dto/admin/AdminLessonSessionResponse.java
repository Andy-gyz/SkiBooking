package com.skibooking.dto.admin;

import java.time.LocalDate;
import java.time.LocalTime;

import com.skibooking.entity.LessonSession;
import com.skibooking.entity.enums.LessonSessionStatus;

public record AdminLessonSessionResponse(
        Long id,
        Long productId,
        String productName,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int bookedCount,
        int availableCount,
        LessonSessionStatus status) {

    public static AdminLessonSessionResponse from(LessonSession session) {
        return new AdminLessonSessionResponse(
                session.getId(),
                session.getProduct().getId(),
                session.getProduct().getName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getCapacity(),
                session.getBookedCount(),
                session.getAvailableCount(),
                session.getStatus());
    }
}
