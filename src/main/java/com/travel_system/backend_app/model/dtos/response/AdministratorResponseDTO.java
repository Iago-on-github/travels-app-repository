package com.travel_system.backend_app.model.dtos.response;

import com.travel_system.backend_app.model.enums.GeneralStatus;

import java.time.LocalDateTime;

public record AdministratorResponseDTO(
        String email,
        String password,
        String name,
        String lastName,
        String telephone,
        String profilePicture,
        GeneralStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
