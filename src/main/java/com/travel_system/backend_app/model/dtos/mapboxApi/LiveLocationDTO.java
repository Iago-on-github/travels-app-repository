package com.travel_system.backend_app.model.dtos.mapboxApi;

public record LiveLocationDTO(double latitude, double longitude, String geometry, double distance) {
}
