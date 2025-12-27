package com.travel_system.backend_app.exceptions;

public class InvalidNotificationStateException extends RuntimeException {
    public InvalidNotificationStateException(String message) {
        super(message);
    }
}
