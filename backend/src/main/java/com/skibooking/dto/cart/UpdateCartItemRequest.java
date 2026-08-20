package com.skibooking.dto.cart;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCartItemRequest(
        @Min(1) @Max(20) int quantity,
        LocalDate bookingDate,
        @Size(max = 30) String vehicleRegistration,
        @Size(max = 50) String vehicleType,
        LocalDate entryDate,
        LocalDate exitDate,
        @Positive Long lessonSessionId,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        @Size(max = 50) String rentalSize,
        @Size(max = 30) String rentalBootSize) {
}
