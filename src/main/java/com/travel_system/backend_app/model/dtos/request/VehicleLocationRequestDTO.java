package com.travel_system.backend_app.model.dtos.request;

import java.util.UUID;

public record VehicleLocationRequestDTO(UUID travelId, Double latitude, Double longitude) {
}
