package com.travel_system.backend_app.events;

import java.util.UUID;

public record StudentProximityEvents(UUID travelId,
                                     UUID studentId,
                                     Double distance,
                                     String zone,
                                     String timestamp,
                                     String alertType) {
}
