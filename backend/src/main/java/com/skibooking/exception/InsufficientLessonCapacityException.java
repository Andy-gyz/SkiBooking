package com.skibooking.exception;

public class InsufficientLessonCapacityException extends RuntimeException {

    public InsufficientLessonCapacityException() {
        super("The lesson session does not have enough remaining capacity.");
    }
}
