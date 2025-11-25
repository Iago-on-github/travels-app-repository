package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.enums.TravelStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TravelResponseDTO(
        UUID id,
        TravelStatus status,
        Driver driver,
        Set<StudentTravel> studentTravel,
        Instant startHourTravel,
        Instant endHourTravel
        ) {
}
