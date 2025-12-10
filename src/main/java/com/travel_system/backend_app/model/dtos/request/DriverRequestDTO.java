package com.travel_system.backend_app.model.dtos.request;

import com.travel_system.backend_app.model.enums.GeneralStatus;

public record DriverRequestDTO(
        String email,
        String password,
        String name,
        String lastName,
        String telephone,
        String profilePicture,
        GeneralStatus status,
        String areaOfActivity
) {
}
