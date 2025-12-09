package com.travel_system.backend_app.model.dtos.mapboxApi;

public record RouteDetailsDTO(
        Double duration,
        Double distance,
        String geometry
) {
}
