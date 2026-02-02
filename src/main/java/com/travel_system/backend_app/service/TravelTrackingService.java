package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.travel_system.backend_app.events.NewLocationReceivedEvents;
import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.listeners.LocationProcessingListener;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.*;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class TravelTrackingService {

    private final TravelRepository travelRepository;
    private final RedisTrackingService redisTrackingService;
    private final MapboxAPIService mapboxAPIService;
    private final RouteCalculationService routeCalculationService;
    private final StudentTravelRepository studentTravelRepository;

    private final ApplicationEventPublisher eventPublisher;

    // usar no lugar de Instant.now() para ajudar nos testes unitários
    private final Clock clock;

    public TravelTrackingService(TravelRepository travelRepository, RedisTrackingService redisTrackingService, MapboxAPIService mapboxAPIService, RouteCalculationService routeCalculationService, StudentTravelRepository studentTravelRepository, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.travelRepository = travelRepository;
        this.redisTrackingService = redisTrackingService;
        this.mapboxAPIService = mapboxAPIService;
        this.routeCalculationService = routeCalculationService;
        this.studentTravelRepository = studentTravelRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // Anota que o motorista passou pela localização atual e libera o celular o mais rápido possível
    public void markDriverCheckpoint(String travelId, String latitude, String longitude) {
        Travel travel = travelRepository.findById(UUID.fromString(travelId))
                .orElseThrow(() -> new TripNotFound("Trip not found"));

        if (travel.getTravelStatus() != TravelStatus.TRAVELLING) {
            throw new TravelException("A viagem não está em andamento");
        }

        // salva no redis como última posição conhecida matendo a distance e o geometry antigos
        LiveLocationDTO liveLocation = redisTrackingService.getLiveLocation(travelId);
        String distance = String.valueOf(liveLocation.distance());
        String geometry = liveLocation.geometry();

        redisTrackingService.storeLiveLocation(travelId, latitude, longitude, distance, geometry);

        // dispara evento de domínio
        NewLocationReceivedEvents event = new NewLocationReceivedEvents(travelId, latitude, longitude, Instant.now(), travel.getTravelStatus());
        eventPublisher.publishEvent(event);
    }

    // Orquestra o sistema de tracking em tempo real, verificando desvios de rota,
    // recalculando o ETA e salvando a localização e os metadados da viagem no Redis
    public void processNewLocation(UUID travelId, Double currentLat, Double currentLng) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Trip not found"));

        if (travel.getTravelStatus() != TravelStatus.TRAVELLING) {
            throw new TravelException("A viagem não está em andamento");
        }

        RouteDeviationDTO routeDeviation = routeCalculationService.isRouteDeviation(currentLat, currentLng, travel.getPolylineRoute());

        RouteDetailsDTO newEtaRecalculateByApi;
        PreviousStateDTO previousEta;

        double newETARecalculateByInternally;

        Double currentDuration;
        Double currentDistance;
        String currentPolyline;

        try {
            // se está fora da rota, chama o metodo para recalcular a distância entre os pontos
            if (routeDeviation.isOffRoute()) {
                newEtaRecalculateByApi = mapboxAPIService.recalculateETA(
                        currentLng,
                        currentLat,
                        travel.getFinalLongitude(),
                        travel.getFinalLatitude());

                currentDuration = newEtaRecalculateByApi.duration();
                currentDistance = newEtaRecalculateByApi.distance();
                currentPolyline = newEtaRecalculateByApi.geometry();

            } else {
                previousEta = redisTrackingService.getPreviousEta(travel.getId().toString());

                long currentTimeMillis = clock.millis();
                long timeElapsedMillis = currentTimeMillis - previousEta.timeStamp();
                double timeElapsedSeconds = (double) timeElapsedMillis / 1000.0;

                newETARecalculateByInternally = previousEta.durationRemaining() - timeElapsedSeconds;

                // nunca deixa ser valor negativo
                newETARecalculateByInternally = Math.max(0.0, newETARecalculateByInternally);

                currentDuration = newETARecalculateByInternally;
                currentDistance = travel.getDistance();
                currentPolyline = travel.getPolylineRoute();
            }
        } catch (Exception e) {
            throw new RecalculateEtaException(e.getMessage(), e.getCause());
        }

        redisTrackingService.storeLiveLocation(
                travel.getId().toString(),
                currentLat.toString(),
                currentLng.toString(),
                currentDuration.toString(),
                currentPolyline);

        redisTrackingService.storeTravelMetadata(
                travel.getId().toString(),
                currentPolyline,
                currentDistance.toString(),
                travel.getTravelStatus().toString()
        );
    }

    // haverá um popup no front que perguntará se o estudante irá participar da viagem
    public void confirmEmbarkOnTravel(UUID studentId, UUID travelId) {
        StudentTravel studentTravel = studentTravelRepository
                .findByStudentIdAndTravelId(studentId, travelId)
                .orElseThrow(() -> new TravelStudentAssociationNotFoundException("Associação travel e student não encontrada"));

        if (studentTravel.isEmbark()) {
            throw new BoardingAlreadyConfirmedException("Embarque já confirmado");
        }

        studentTravel.setEmbark(true);
        studentTravelRepository.save(studentTravel);
    }

    // endpoint de fastview - provê a loc do driver
    public LiveLocationDTO getDriverPosition(UUID travelId) {
        Travel travel = travelRepository.findById(travelId).orElseThrow(() -> new EntityNotFoundException("Viagem não encontrada: " + travelId));

        if (!(travel.getTravelStatus() == TravelStatus.TRAVELLING)) {
            throw new TravelException("Viagem " + travelId + " não está em andamento.");
        }

        LiveLocationDTO liveCoordinates = extractLiveCoordinates(travelId);

        String geometry = liveCoordinates.geometry();
        double distance = liveCoordinates.distance();
        Double lastCalcLatitude = liveCoordinates.lastCalcLat();
        Double lastCalcLongitude = liveCoordinates.lastCalcLng();

        RouteDeviationDTO isDeviation = routeCalculationService.isRouteDeviation(
                liveCoordinates.lastCalcLat(),
                liveCoordinates.lastCalcLng(),
                geometry);

        RouteDetailsDTO routeDetailsDTO;

        if (geometry == null || isDeviation.isOffRoute()) {
            routeDetailsDTO = mapboxAPIService.calculateRoute(
                    liveCoordinates.longitude(),
                    liveCoordinates.latitude(),
                    travel.getFinalLongitude(),
                    travel.getFinalLatitude());

            geometry = routeDetailsDTO.geometry();
            distance = routeDetailsDTO.distance();

            lastCalcLatitude = liveCoordinates.latitude();
            lastCalcLongitude = liveCoordinates.longitude();

            redisTrackingService.storeLiveLocation(
                    String.valueOf(travelId),
                    String.valueOf(lastCalcLatitude),
                    String.valueOf(lastCalcLongitude),
                    String.valueOf(distance),
                    geometry);
        }

        return new LiveLocationDTO(
                liveCoordinates.latitude(),
                liveCoordinates.longitude(),
                geometry,
                distance,
                lastCalcLatitude,
                lastCalcLongitude);
    }

    // MÉTODOS AUXILIARES
    private LiveLocationDTO extractLiveCoordinates(UUID travelId) {
        LiveLocationDTO currentLocation = redisTrackingService.getLiveLocation(String.valueOf(travelId));

        try {
            double currentLatitude = currentLocation.latitude();
            double currentLongitude = currentLocation.longitude();

            return new LiveLocationDTO(
                    currentLatitude,
                    currentLongitude,
                    currentLocation.geometry(),
                    currentLocation.distance(),
                    currentLocation.lastCalcLat(),
                    currentLocation.lastCalcLng());
        } catch (Exception e) {
            throw new LiveLocationDataNotFoundException("Dados de rastreamento corrompidos ou inválidos: " + e.getMessage());
        }
    }
}
