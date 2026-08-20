package com.skibooking.dto.admin;

import java.time.LocalDate;
import java.time.LocalTime;

import com.skibooking.entity.enums.LessonSessionStatus;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AdminLessonSessionRequest(
        @NotNull @Positive Long productId,
        @NotNull @FutureOrPresent LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Positive int capacity,
        @NotNull LessonSessionStatus status) {
}
