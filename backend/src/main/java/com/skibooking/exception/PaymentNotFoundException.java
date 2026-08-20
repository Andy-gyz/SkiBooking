package com.skibooking.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException() {
        super("The payment could not be found.");
    }
}
