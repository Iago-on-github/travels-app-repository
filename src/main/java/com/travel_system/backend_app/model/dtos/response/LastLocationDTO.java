package com.travel_system.backend_app.model.dtos.response;

import java.time.Instant;

public record LastLocationDTO(double latitude,
                              double longitude,
                              Instant timestamp,
                              double avgSpeed,
                              Instant lastTrafficAlertTimestamp) {
}
