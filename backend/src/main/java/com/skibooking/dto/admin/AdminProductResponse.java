package com.skibooking.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

import com.skibooking.dto.product.ResortSummaryResponse;
import com.skibooking.entity.Product;
import com.skibooking.entity.enums.ProductCategory;

public record AdminProductResponse(
        Long id,
        ResortSummaryResponse resort,
        String name,
        ProductCategory category,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminProductResponse from(Product product) {
        return new AdminProductResponse(
                product.getId(),
                ResortSummaryResponse.from(product.getResort()),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getPrice(),
                "AUD",
                product.getImageUrl(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
