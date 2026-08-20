package com.skibooking.dto.admin;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AdminLessonSessionBulkRequest(
        @NotNull Long productId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty Set<DayOfWeek> daysOfWeek,
        @NotEmpty List<@Valid AdminLessonSessionSlotRequest> slots) {
}
