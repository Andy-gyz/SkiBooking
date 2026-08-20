package com.skibooking.exception;

public class CartNotFoundException extends RuntimeException {

    public CartNotFoundException() {
        super("The cart was not found or you do not have access to it.");
    }
}
