package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.Role;

import java.time.LocalDateTime;

public record AdministratorResponseDTO(
        String email,
        String password,
        String name,
        String lastName,
        Role role,
        String telephone,
        String profilePicture,
        GeneralStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
