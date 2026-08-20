package com.skibooking.dto.product;

import com.skibooking.entity.Resort;

public record ResortResponse(
        Long id,
        String name,
        String location,
        String description,
        String imageUrl) {

    public static ResortResponse from(Resort resort) {
        return new ResortResponse(
                resort.getId(),
                resort.getName(),
                resort.getLocation(),
                resort.getDescription(),
                resort.getImageUrl());
    }
}
