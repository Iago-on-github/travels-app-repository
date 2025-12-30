package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.GeoPosition;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.response.DistanceResponseDTO;
import com.travel_system.backend_app.model.dtos.response.NotificationStateDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public PushNotificationService(TravelTrackingService travelTrackingService, TravelService travelService, RouteCalculationService routeCalculationService, RedisNotificationService redisNotificationService) {
        this.travelTrackingService = travelTrackingService;
        this.travelService = travelService;
        this.routeCalculationService = routeCalculationService;
        this.redisNotificationService = redisNotificationService;
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

            Boolean shouldPushNotification = redisNotificationService.verifyNotificationState(
                    travelId,
                    student.studentId(),
                    distance,
                    readNotificationState);

            if (shouldPushNotification) {
                // disparara evento
                redisNotificationService.updateNotificationState(travelId, student.studentId(), new NotificationStateDTO(
                        zone,
                        distance.toString(),
                        lastNotificationAt,
                        String.valueOf(Instant.now())
                        ));
                // verificar o método verify do redis, para ver se ele salvaŕa a parte do NEAR e FAR
                // realizar a orquestração com RabbitMQ
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
