package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.*;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;
import java.util.UUID;

@Service
public class TravelTrackingService {

    private final TravelRepository travelRepository;
    private final RedisTrackingService redisTrackingService;
    private final MapboxAPIService mapboxAPIService;
    private final RouteCalculationService routeCalculationService;
    private final StudentTravelRepository studentTravelRepository;

    // usar no lugar de Instant.now() para ajudar nos testes unitários
    private final Clock clock;

    @Autowired
    public TravelTrackingService(TravelRepository travelRepository, RedisTrackingService redisTrackingService, MapboxAPIService mapboxAPIService, RouteCalculationService routeCalculationService, StudentTravelRepository studentTravelRepository, Clock clock) {
        this.travelRepository = travelRepository;
        this.redisTrackingService = redisTrackingService;
        this.mapboxAPIService = mapboxAPIService;
        this.routeCalculationService = routeCalculationService;
        this.studentTravelRepository = studentTravelRepository;
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
                currentDuration.toString());

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

        LiveCoordinates liveCoordinates = extractLiveCoordinates(travelId);

        RouteDetailsDTO routeDetailsDTO = mapboxAPIService.calculateRoute(
                liveCoordinates.currentLongitude(),
                liveCoordinates.currentLatitude(),
                travel.getFinalLongitude(),
                travel.getFinalLatitude());

        return new LiveLocationDTO(liveCoordinates.currentLatitude(), liveCoordinates.currentLongitude(), routeDetailsDTO.geometry(), routeDetailsDTO.distance());
    }

    private LiveCoordinates extractLiveCoordinates(UUID travelId) {
        Map<String, String> currentLocation = redisTrackingService.getLiveLocation(String.valueOf(travelId));

        if (currentLocation.isEmpty()) {
            throw new LiveLocationDataNotFoundException("Nenhum dado de rastreamento em tempo real encontrado para a viagem." + currentLocation);
        }

        if (!currentLocation.containsKey("lat") || !currentLocation.containsKey("lng")) {
            throw new LiveLocationDataNotFoundException("Dados de longitude/latitude ausentes no rastreamento.");
        }

        try {
            double currentLatitude = Double.parseDouble(currentLocation.get("lat"));
            double currentLongitude = Double.parseDouble(currentLocation.get("lng"));

            return new LiveCoordinates(currentLatitude, currentLongitude);
        } catch (Exception e) {
            throw new LiveLocationDataNotFoundException("Dados de rastreamento corrompidos ou inválidos: " + e.getMessage());
        }
    }
}
