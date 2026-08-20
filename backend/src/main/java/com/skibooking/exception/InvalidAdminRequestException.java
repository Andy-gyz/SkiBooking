package com.skibooking.exception;

public class InvalidAdminRequestException extends RuntimeException {

    public InvalidAdminRequestException(String message) {
        super(message);
    }
}
