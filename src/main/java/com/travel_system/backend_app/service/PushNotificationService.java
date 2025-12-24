package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

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

    // será o orquestrador
    public void checkProximityAlerts(UUID travelId ) {
        LiveLocationDTO driverPosition = travelTrackingService.getDriverPosition(travelId);

    }

    private Boolean thresholdLimits(UUID travelId, LiveLocationDTO driverPosition) {
        Set<StudentTravelResponseDTO> linkedStudentTravel = travelService.linkedStudentTravel(travelId);

        linkedStudentTravel.forEach(student -> {
            Double distanceInMeters = routeCalculationService.calculateHaversineDistanceInMeters(
                    driverPosition.longitude(),
                    driverPosition.latitude(),
                    student.position().getLongitude(),
                    student.position().getLatitude());

        });
        return false;
    }
}
