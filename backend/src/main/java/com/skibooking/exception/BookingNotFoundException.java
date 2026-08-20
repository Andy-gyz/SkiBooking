package com.skibooking.exception;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException() {
        super("The booking was not found or you do not have access to it.");
    }
}
