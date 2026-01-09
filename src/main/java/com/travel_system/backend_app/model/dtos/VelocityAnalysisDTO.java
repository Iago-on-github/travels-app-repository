package com.travel_system.backend_app.model.dtos;

import com.travel_system.backend_app.model.enums.MovementState;

import java.time.Duration;

public record VelocityAnalysisDTO(Double AverageSpeed,
                                  Long timeElapsed,
                                  Double distanceBetweenPings,
                                  Double newETA,
                                  MovementState movementState) {
}
