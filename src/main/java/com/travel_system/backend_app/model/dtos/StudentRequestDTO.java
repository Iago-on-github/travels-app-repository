package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.model.enums.StatusStudent;

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
