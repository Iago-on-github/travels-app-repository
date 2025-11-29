package com.travel_system.backend_app.model.dtos.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DriverResponseDTO(
        UUID id,
        String name,
        String lastName,
        String email,
        String telephone,
        LocalDateTime createdAt,
        String areaOfActivity,
        Integer totalTrips
) {
}
