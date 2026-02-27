package com.travel_system.backend_app.listeners;

import com.travel_system.backend_app.events.NewLocationReceivedEvents;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mesageria.MessagingDTO;
import com.travel_system.backend_app.model.dtos.request.VehicleLocationRequestDTO;
import com.travel_system.backend_app.repository.CityRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import com.travel_system.backend_app.service.GpsService;
import com.travel_system.backend_app.service.PushNotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class GpsMessagingListener {
    private final GpsService gpsService;
    private final TravelRepository travelRepository;

    private static final Logger logger = LoggerFactory.getLogger(GpsMessagingListener.class);

    public GpsMessagingListener(GpsService gpsService, TravelRepository travelRepository) {
        this.gpsService = gpsService;
        this.travelRepository = travelRepository;
    }

    @EventListener
    @Async
    public void handleGpsToMessaging(NewLocationReceivedEvents locationReceivedEvents) {
        UUID travelId = locationReceivedEvents.travelId();
        Double latitude = locationReceivedEvents.latitude();
        Double longitude = locationReceivedEvents.longitude();
        Double speed = locationReceivedEvents.speed();
        Double heading = locationReceivedEvents.heading();
        Instant timestamp = locationReceivedEvents.timestamp();

        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new EntityNotFoundException("Viagem não encontrada" + travelId));

        logger.info("[handleGpsToMessaging] entidade Travel encontrada, seguindo adiante com o processamento do gps no rabbitmq");

        String cityName = travel.getCity().getName().toLowerCase();
        String cityNameFormatted = cityName.replace(" ", "_").trim();

        MessagingDTO messagingDTO = new MessagingDTO(latitude, longitude, heading, speed, timestamp, travelId);

        gpsService.sendLocalization(cityNameFormatted, travelId, messagingDTO);
    }

}
