package com.skibooking.exception;

public class VerificationCodeCooldownException extends RuntimeException {

    public VerificationCodeCooldownException() {
        super("Please wait before requesting another verification code.");
    }
}
