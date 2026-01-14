package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.Priority;

import java.time.Instant;
import java.util.UUID;

public record VehicleMovementNotificationDTO(UUID travelId, MovementState type, Instant timestamp, String message, Priority priority) {
}
