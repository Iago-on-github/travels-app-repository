package com.travel_system.backend_app.listeners;

import com.travel_system.backend_app.events.NewLocationReceivedEvents;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.request.VehicleLocationRequestDTO;
import com.travel_system.backend_app.repository.TravelRepository;
import com.travel_system.backend_app.service.PushNotificationService;
import com.travel_system.backend_app.service.TravelTrackingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class LocationProcessingListener {
    private final TravelTrackingService travelTrackingService;
    private final PushNotificationService pushNotificationService;
    private final TravelRepository travelRepository;

    public LocationProcessingListener(TravelTrackingService travelTrackingService, PushNotificationService pushNotificationService, TravelRepository travelRepository) {
        this.travelTrackingService = travelTrackingService;
        this.pushNotificationService = pushNotificationService;
        this.travelRepository = travelRepository;
    }

    @Async
    @EventListener
    @Transactional(readOnly = true)
    public void handleLocationProcessing(NewLocationReceivedEvents locationReceivedEvents) {
        UUID travelId = locationReceivedEvents.travelId();
        Double latitude = locationReceivedEvents.latitude();
        Double longitude = locationReceivedEvents.longitude();

        Travel travel = travelRepository.findByIdWithStudents(travelId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem não encontrada: " + travelId));

        travelTrackingService.processNewLocation(new VehicleLocationRequestDTO(travelId, latitude, longitude));

        // 2. Processa Alertas de Proximidade e Movimento (O "cérebro" das notificações)
        // Note: o checkProximityAlerts agora será disparado a cada novo ping de GPS
        pushNotificationService.checkProximityAlerts(new VehicleLocationRequestDTO(travelId, latitude, longitude));
        pushNotificationService.processVehicleMovement(new VehicleLocationRequestDTO(travelId, latitude, longitude));
    }
}
