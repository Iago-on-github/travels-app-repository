package com.travel_system.backend_app.model.dtos.response;

import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.model.enums.GeneralStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StudentResponseDTO(
        UUID id,
        String name,
        String lastName,
        String email,
        String telephone,
        LocalDateTime createdAt,
        InstitutionType institutionType,
        String course,
        GeneralStatus status
        ) {

}
