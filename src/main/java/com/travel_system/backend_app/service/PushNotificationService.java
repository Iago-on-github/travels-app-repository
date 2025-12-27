package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PushNotificationService {

    private final TravelTrackingService travelTrackingService;
    private final TravelService travelService;
    private final RouteCalculationService routeCalculationService;

    private final Double FAR_DISTANCE = 1000.0;

    private final Double FAR_NOTIFY = 200.0;

    public PushNotificationService(TravelTrackingService travelTrackingService, TravelService travelService, RouteCalculationService routeCalculationService) {
        this.travelTrackingService = travelTrackingService;
        this.travelService = travelService;
        this.routeCalculationService = routeCalculationService;
    }

    // será o orchestrator
    public void checkProximityAlerts(UUID travelId ) {

    }

    // fazer o método checkProximityAlerts com os dados do redis
    protected Map<UUID, Double> distanceBetweenPositions(UUID travelId, LiveLocationDTO driverPosition) {
        Set<StudentTravelResponseDTO> linkedStudentTravel = travelService.linkedStudentTravel(travelId);

        return linkedStudentTravel.stream()
                .collect(Collectors.toMap(StudentTravelResponseDTO::studentId,
                        student -> routeCalculationService.calculateHaversineDistanceInMeters(
                                driverPosition.longitude(),
                                driverPosition.latitude(),
                                student.position().getLongitude(),
                                student.position().getLatitude())));
    }
}
