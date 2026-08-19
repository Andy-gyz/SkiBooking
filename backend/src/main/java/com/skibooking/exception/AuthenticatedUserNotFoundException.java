package com.skibooking.exception;

public class AuthenticatedUserNotFoundException extends RuntimeException {

    public AuthenticatedUserNotFoundException() {
        super("The authenticated user no longer exists.");
    }
}
