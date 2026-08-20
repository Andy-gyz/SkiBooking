package com.skibooking.dto.admin;

import java.math.BigDecimal;

import com.skibooking.entity.enums.ProductCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminProductRequest(
        @NotNull @Positive Long resortId,
        @NotBlank @Size(max = 150) String name,
        @NotNull ProductCategory category,
        String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Size(max = 500) String imageUrl,
        boolean active) {
}
