package com.travel_system.backend_app.customExceptions;

public class NoSuchCoordinates extends RuntimeException {
    public NoSuchCoordinates(String message) {
        super(message);
    }
}
