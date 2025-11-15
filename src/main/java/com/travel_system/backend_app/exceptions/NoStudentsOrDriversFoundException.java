package com.travel_system.backend_app.exceptions;

public class NoStudentsOrDriversFoundException extends RuntimeException {
    public NoStudentsOrDriversFoundException(String message) {
        super(message);
    }
}
