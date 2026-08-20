package com.skibooking.dto.cart;

import java.math.BigDecimal;
import java.util.List;

import com.skibooking.entity.Cart;
import com.skibooking.entity.enums.CartStatus;

public record CartResponse(
        Long id,
        CartStatus status,
        int itemCount,
        BigDecimal subtotal,
        BigDecimal total,
        String currency,
        List<CartItemResponse> items) {

    public static CartResponse from(Cart cart, List<CartItemResponse> items) {
        int itemCount = items.stream().mapToInt(CartItemResponse::quantity).sum();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                cart.getStatus(),
                itemCount,
                subtotal,
                subtotal,
                "AUD",
                items);
    }
}
