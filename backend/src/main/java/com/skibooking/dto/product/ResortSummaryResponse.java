package com.skibooking.dto.product;

import com.skibooking.entity.Resort;

public record ResortSummaryResponse(Long id, String name, String location) {

    public static ResortSummaryResponse from(Resort resort) {
        return new ResortSummaryResponse(resort.getId(), resort.getName(), resort.getLocation());
    }
}
