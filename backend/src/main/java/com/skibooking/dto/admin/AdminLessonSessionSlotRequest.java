package com.skibooking.dto.admin;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminLessonSessionSlotRequest(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Min(1) int capacity) {
}
