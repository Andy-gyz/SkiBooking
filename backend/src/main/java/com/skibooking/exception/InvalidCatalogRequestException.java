package com.skibooking.exception;

public class InvalidCatalogRequestException extends RuntimeException {

    public InvalidCatalogRequestException(String message) {
        super(message);
    }
}
