package com.travel_system.backend_app.exceptions.catchException;

import com.travel_system.backend_app.exceptions.InvalidJwtAuthenticationToken;
import com.travel_system.backend_app.exceptions.standardError.StandardError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;

@ControllerAdvice
public class CapturedAndCustomizedExceptions {

    @ExceptionHandler(InvalidJwtAuthenticationToken.class)
    public final ResponseEntity<StandardError> invalidJwtAuthenticationException(Exception ex, WebRequest webRequest) {
        StandardError standardError = new StandardError(
                LocalDate.now(),
                HttpStatus.FORBIDDEN.value(),
                ex.getMessage(),
                webRequest.getDescription(false));

        return new ResponseEntity<>(standardError, HttpStatus.FORBIDDEN);
    }
}
