package com.travel_system.backend_app.customExceptions;

public class NoStudentsOrDriversFoundException extends RuntimeException {
    public NoStudentsOrDriversFoundException(String message) {
        super(message);
    }
}
