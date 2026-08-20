package com.skibooking.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.skibooking.entity.BookingItem;
import com.skibooking.entity.enums.ProductCategory;

public record BookingItemResponse(
        Long id,
        Long productId,
        Long lessonSessionId,
        String productName,
        ProductCategory category,
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

    public static BookingItemResponse from(BookingItem item) {
        return new BookingItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getLessonSession() == null ? null : item.getLessonSession().getId(),
                item.getProductName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal(),
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
