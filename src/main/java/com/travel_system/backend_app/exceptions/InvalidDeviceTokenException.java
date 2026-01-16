package com.travel_system.backend_app.exceptions;

public class InvalidDeviceTokenException extends RuntimeException {
    public InvalidDeviceTokenException(String message) {
        super(message);
    }
}
