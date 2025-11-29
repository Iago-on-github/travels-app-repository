package com.travel_system.backend_app.model.dtos.request;

public record RegisterUserRequestDTO(
        String name,
        String email,
        String password
) {
}
