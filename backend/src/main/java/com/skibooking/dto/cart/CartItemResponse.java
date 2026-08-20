package com.skibooking.dto.cart;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.skibooking.dto.product.ProductResponse;
import com.skibooking.entity.CartItem;

public record CartItemResponse(
        Long id,
        ProductResponse product,
        Long lessonSessionId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        LocalDate bookingDate,
        String vehicleRegistration,
        String vehicleType,
        LocalDate entryDate,
        LocalDate exitDate,
        LocalDate rentalStartDate,
        LocalDate rentalEndDate,
        String rentalSize,
        String rentalBootSize) {

    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                ProductResponse.from(item.getProduct()),
                item.getLessonSession() == null ? null : item.getLessonSession().getId(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
                item.getBookingDate(),
                item.getVehicleRegistration(),
                item.getVehicleType(),
                item.getEntryDate(),
                item.getExitDate(),
                item.getRentalStartDate(),
                item.getRentalEndDate(),
                item.getRentalSize(),
                item.getRentalBootSize());
    }
}
