package com.travel_system.backend_app.events;

import com.travel_system.backend_app.model.enums.TravelStatus;

import java.time.Instant;

public record NewLocationReceivedEvents(String travelId, String latitude, String longitude, Instant timestamp, TravelStatus status) {
}
