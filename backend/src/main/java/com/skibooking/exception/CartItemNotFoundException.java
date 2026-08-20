package com.skibooking.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException() {
        super("The cart item was not found in this cart.");
    }
}
