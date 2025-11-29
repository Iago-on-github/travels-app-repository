package com.travel_system.backend_app.model.dtos.request;

import com.travel_system.backend_app.model.enums.InstitutionType;

public record StudentRequestDTO(
        String email,
        String password,
        String name,
        String lastName,
        String telephone,
        String profilePicture,
        InstitutionType institutionType,
        String course) {
}
