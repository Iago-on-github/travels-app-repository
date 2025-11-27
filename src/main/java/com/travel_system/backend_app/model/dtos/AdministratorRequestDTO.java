package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.enums.Role;

public record AdministratorRequestDTO(
        String email,
        String password,
        String name,
        String lastName,
        String telephone,
        String profilePicture
) {
}
