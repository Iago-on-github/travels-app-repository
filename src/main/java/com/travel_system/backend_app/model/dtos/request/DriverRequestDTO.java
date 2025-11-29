package com.travel_system.backend_app.model.dtos.request;

public record DriverRequestDTO(
        String email,
        String password,
        String name,
        String lastName,
        String telephone,
        String profilePicture,
        String areaOfActivity
) {
}
