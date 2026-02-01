package com.travel_system.backend_app.events;

import com.travel_system.backend_app.model.dtos.VelocityAnalysisDTO;
import com.travel_system.backend_app.model.enums.ShouldNotify;

import java.util.UUID;

public record VehicleMovementEvents(UUID travelId,
                                    VelocityAnalysisDTO velocityAnalysis,
                                    ShouldNotify decision,
                                    UUID traceId) {
}
