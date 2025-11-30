package com.travel_system.backend_app.model.dtos.response;

import java.time.Instant;
import java.time.LocalDateTime;

public record LoginResponseDTO(
        String username,
        Boolean authenticated,
        Instant created,
        Instant expiration,
        String accessToken,
        String refreshToken) {
}
