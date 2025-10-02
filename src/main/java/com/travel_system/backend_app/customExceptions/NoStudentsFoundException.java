package com.travel_system.backend_app.customExceptions;

public class NoStudentsFoundException extends RuntimeException {
    public NoStudentsFoundException(String message) {
        super(message);
    }
}
