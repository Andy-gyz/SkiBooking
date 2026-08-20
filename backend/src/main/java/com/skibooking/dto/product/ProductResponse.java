package com.skibooking.dto.product;

import java.math.BigDecimal;

import com.skibooking.entity.Product;
import com.skibooking.entity.enums.ProductCategory;

public record ProductResponse(
        Long id,
        ResortSummaryResponse resort,
        String name,
        ProductCategory category,
        String description,
        BigDecimal price,
        String currency,
        String imageUrl) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                ResortSummaryResponse.from(product.getResort()),
                product.getName(),
                product.getCategory(),
                product.getDescription(),
                product.getPrice(),
                "AUD",
                product.getImageUrl());
    }
}
