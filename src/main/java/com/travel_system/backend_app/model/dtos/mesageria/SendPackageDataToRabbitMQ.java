package com.travel_system.backend_app.model.dtos.mesageria;

import java.util.UUID;

public record SendPackageDataToRabbitMQ(UUID travelId,
                                        UUID studentId,
                                        Double distance,
                                        String zone,
                                        String timestamp,
                                        String alertType) {
}
