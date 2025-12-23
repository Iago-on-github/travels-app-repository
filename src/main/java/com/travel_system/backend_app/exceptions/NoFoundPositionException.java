package com.travel_system.backend_app.exceptions;

public class NoFoundPositionException extends RuntimeException {
    public NoFoundPositionException(String message) {
        super(message);
    }
}
