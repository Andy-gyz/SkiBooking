package com.skibooking.exception;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("The cart must contain at least one item before checkout.");
    }
}
