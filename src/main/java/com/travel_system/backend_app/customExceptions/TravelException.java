package com.travel_system.backend_app.customExceptions;

public class TravelException extends RuntimeException {
    public TravelException(String msg) {
        super(msg);
    }
}
