package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.RecalculateEtaException;
import com.travel_system.backend_app.exceptions.TravelException;
import com.travel_system.backend_app.exceptions.TripNotFound;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.PreviousStateDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.TravelRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class TravelTrackingService {

    private TravelRepository travelRepository;
    private RedisTrackingService redisTrackingService;
    private MapboxAPIService mapboxAPIService;
    private RouteCalculationService routeCalculationService;

    // usar no lugar de Instant.now() para ajudar nos testes unitários
    private Clock clock;

    @Autowired
    public TravelTrackingService(TravelRepository travelRepository, RedisTrackingService redisTrackingService, MapboxAPIService mapboxAPIService, RouteCalculationService routeCalculationService, Clock clock) {
        this.travelRepository = travelRepository;
        this.redisTrackingService = redisTrackingService;
        this.mapboxAPIService = mapboxAPIService;
        this.routeCalculationService = routeCalculationService;
        this.clock = clock;
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
                newEtaRecalculateByApi = mapboxAPIService.recalculateETA(currentLng, currentLat, travel.getFinalLatitude(), travel.getFinalLongitude());

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
                currentDuration.toString());

        redisTrackingService.storeTravelMetadata(
                travel.getId().toString(),
                currentPolyline,
                currentDistance.toString(),
                travel.getTravelStatus().toString()
        );
    }
}
