package com.travel_system.backend_app.exceptions;

public class EmptyMandatoryFieldsFound extends RuntimeException {

    public EmptyMandatoryFieldsFound(String message) {
        super(message);
    }
}
