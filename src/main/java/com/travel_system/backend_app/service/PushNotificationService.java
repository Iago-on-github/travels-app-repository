package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.GeoPosition;
import com.travel_system.backend_app.model.dtos.SendPackageDataToRabbitMQ;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.response.DistanceResponseDTO;
import com.travel_system.backend_app.model.dtos.response.NotificationStateDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import com.travel_system.backend_app.utils.RabbitMQProducer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PushNotificationService {

    private final TravelTrackingService travelTrackingService;
    private final TravelService travelService;
    private final RouteCalculationService routeCalculationService;
    private final RedisNotificationService redisNotificationService;
    private final RabbitMQProducer rabbitMQProducer;

    public PushNotificationService(TravelTrackingService travelTrackingService, TravelService travelService, RouteCalculationService routeCalculationService, RedisNotificationService redisNotificationService, RabbitMQProducer rabbitMQProducer) {
        this.travelTrackingService = travelTrackingService;
        this.travelService = travelService;
        this.routeCalculationService = routeCalculationService;
        this.redisNotificationService = redisNotificationService;
        this.rabbitMQProducer = rabbitMQProducer;
    }

    // será o orchestrator
    public void checkProximityAlerts(UUID travelId ) {
        LiveLocationDTO driverPosition = travelTrackingService.getDriverPosition(travelId);
        Set<StudentTravelResponseDTO> linkedStudentTravel = travelService.linkedStudentTravel(travelId);
        List<DistanceResponseDTO> differencePosition = distanceBetweenPositions(travelId, driverPosition);

        Map<UUID, Double> distances = differencePosition.stream()
                .collect(Collectors.toMap(DistanceResponseDTO::studentId, DistanceResponseDTO::distance));

        linkedStudentTravel.forEach(student -> {
            NotificationStateDTO readNotificationState = redisNotificationService.readNotificationState(travelId, student.studentId());

            Double distance = distances.get(student.studentId());
            String zone = distance >= 1000 ? "FAR" : "NEAR";
            String lastNotificationAt = String.valueOf(Instant.now().toEpochMilli());
            String timestamp = String.valueOf(Instant.now());

            Boolean shouldPushNotification = redisNotificationService.verifyNotificationState(
                    travelId,
                    student.studentId(),
                    distance,
                    readNotificationState);

            String alertType = "";

            // seta o alertyType
            if (!zone.equals(readNotificationState.zone())) {
                alertType = "ZONE_CHANGED";
            } else {
                long elapsedMinutes = Instant.parse(lastNotificationAt).toEpochMilli()
                        - Instant.parse(readNotificationState.lastNotificationAt()).toEpochMilli();

                if (elapsedMinutes >= 720000) {
                    alertType = "TIME_ELAPSED";
                } else {
                    double lastDistance = Double.parseDouble(readNotificationState.lastDistanceNotified());
                    double deltaDistance = Math.abs(distance - lastDistance);

                    double step = zone.equals("FAR") ? 200.0 : 30.0;
                    if (deltaDistance >= step) {
                        alertType = "DISTANCE_STEP_REACHED";
                    }
                }
            }

            if (shouldPushNotification) {
                // manda evento ao rabbitMQ
                rabbitMQProducer.sendMessage(new SendPackageDataToRabbitMQ(
                        travelId,
                        student.studentId(),
                        distance, zone,
                        timestamp,
                        alertType));

                // update no redis
                redisNotificationService.updateNotificationState(travelId, student.studentId(),
                        new NotificationStateDTO(zone,
                        distance.toString(),
                        lastNotificationAt,
                        timestamp));
            }
        });

    }

    // distance between driver and student
    protected List<DistanceResponseDTO> distanceBetweenPositions(UUID travelId, LiveLocationDTO driverPosition) {
        Set<StudentTravelResponseDTO> linkedStudentTravel = travelService.linkedStudentTravel(travelId);

        return linkedStudentTravel.stream()
                .map(student -> {
                    double distance = routeCalculationService.calculateHaversineDistanceInMeters(
                            driverPosition.latitude(),
                            driverPosition.longitude(),
                            student.position().getLatitude(),
                            student.position().getLongitude()
                    );
                    return new DistanceResponseDTO(student.studentId(), distance);
                })
                .toList();
    }
}
