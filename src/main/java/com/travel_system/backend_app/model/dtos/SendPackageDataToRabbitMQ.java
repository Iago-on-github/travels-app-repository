package com.travel_system.backend_app.model.dtos;

import java.time.Instant;
import java.util.UUID;

public record SendPackageDataToRabbitMQ(UUID travelId,
                                        UUID studentId,
                                        Double distance,
                                        String zone,
                                        String timestamp,
                                        String alertType) {
}
