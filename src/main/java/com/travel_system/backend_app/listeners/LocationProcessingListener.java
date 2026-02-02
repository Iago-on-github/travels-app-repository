package com.travel_system.backend_app.listeners;

import com.travel_system.backend_app.events.NewLocationReceivedEvents;
import com.travel_system.backend_app.service.PushNotificationService;
import com.travel_system.backend_app.service.TravelTrackingService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class LocationProcessingListener {
    private final TravelTrackingService travelTrackingService;
    private final PushNotificationService pushNotificationService;

    public LocationProcessingListener(TravelTrackingService travelTrackingService, PushNotificationService pushNotificationService) {
        this.travelTrackingService = travelTrackingService;
        this.pushNotificationService = pushNotificationService;
    }

    @Async
    @EventListener
    public void handleLocationProcessing(NewLocationReceivedEvents locationReceivedEvents) {
        UUID travelId = locationReceivedEvents.travelId();
        Double latitude = locationReceivedEvents.latitude();
        Double longitude = locationReceivedEvents.longitude();

        travelTrackingService.processNewLocation(travelId, latitude, longitude);

        // 2. Processa Alertas de Proximidade e Movimento (O "cérebro" das notificações)
        // Note: o checkProximityAlerts agora será disparado a cada novo ping de GPS
        pushNotificationService.checkProximityAlerts(travelId, latitude, longitude);
        pushNotificationService.processVehicleMovement(travelId, latitude, longitude);
    }
}
