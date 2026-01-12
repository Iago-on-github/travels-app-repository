package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.TravelException;
import com.travel_system.backend_app.exceptions.TripNotFound;
import com.travel_system.backend_app.model.GeoPosition;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.AnalyzeMovementStateDTO;
import com.travel_system.backend_app.model.dtos.SendPackageDataToRabbitMQ;
import com.travel_system.backend_app.model.dtos.VelocityAnalysisDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.PreviousStateDTO;
import com.travel_system.backend_app.model.dtos.response.DistanceResponseDTO;
import com.travel_system.backend_app.model.dtos.response.LastLocationDTO;
import com.travel_system.backend_app.model.dtos.response.NotificationStateDTO;
import com.travel_system.backend_app.model.dtos.response.StudentTravelResponseDTO;
import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.ShouldNotify;
import com.travel_system.backend_app.repository.TravelRepository;
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
    private final RedisTrackingService redisTrackingService;
    private final TravelRepository travelRepository;

    public PushNotificationService(TravelTrackingService travelTrackingService, TravelService travelService, RouteCalculationService routeCalculationService, RedisNotificationService redisNotificationService, RabbitMQProducer rabbitMQProducer, RedisTrackingService redisTrackingService, TravelRepository travelRepository) {
        this.travelTrackingService = travelTrackingService;
        this.travelService = travelService;
        this.routeCalculationService = routeCalculationService;
        this.redisNotificationService = redisNotificationService;
        this.rabbitMQProducer = rabbitMQProducer;
        this.redisTrackingService = redisTrackingService;
        this.travelRepository = travelRepository;
    }

    /*
    gera pushs de notificações por distância <aluno - ônibus>
    ex.: Ônibus está há 200M de você
    */
    public void checkProximityAlerts(UUID travelId) {
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
                        distance,
                        zone,
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

    // usa analyzeVehicleMovement e decide se deve notificar
    // implementar @async dps
    public ShouldNotify shouldSendNotification(String travelId, VelocityAnalysisDTO velocityAnalysis, AnalyzeMovementStateDTO analyzeMovementState) {
        // verificar mudanças de estado
        AnalyzeMovementStateDTO lastMovementState = redisTrackingService.getLastMovementState(travelId);
        MovementState actualMovementState = velocityAnalysis.movementState();

        // primeiro ciclo: não notificar
        if (lastMovementState == null || lastMovementState.movementState() == null || lastMovementState.stateStartedAt() == null || lastMovementState.lastNotificationSendAt() == null) {
            return ShouldNotify.SHOULD_NO_NOTIFY;
        }

        Instant now = Instant.now();

        final long STATE_TIME_LIMIT_MS = 4_000;
        final long NOTIFICATION_COOLDOWN_MS = 12_000;

        // comparar estados
        // se o estado mudou, ainda nao notifica
        if (!actualMovementState.equals(lastMovementState.movementState())) {
            return ShouldNotify.SHOULD_NO_NOTIFY;
        }

        if (actualMovementState.equals(MovementState.NORMAL)) return ShouldNotify.SHOULD_NO_NOTIFY;

        long durationOnState = now.toEpochMilli() - lastMovementState.stateStartedAt().toEpochMilli();
        long timeSinceLastNotification = lastMovementState.lastEtaNotificationAt().toEpochMilli();

        boolean stayedLongEnough = durationOnState >= STATE_TIME_LIMIT_MS;
        boolean cooldownExpired = timeSinceLastNotification >= NOTIFICATION_COOLDOWN_MS;

        if (stayedLongEnough && cooldownExpired) {
            if (actualMovementState.equals(MovementState.SLOW)) {
                return ShouldNotify.SHOULD_NOTIFY_SLOW;
            }
            if (actualMovementState.equals(MovementState.STOPPED)) {
                return ShouldNotify.SHOULD_NOTIFY_STOPPED;
            }
        }
        return ShouldNotify.SHOULD_NO_NOTIFY;
    }

    /*
    gera pushs de notificações por anomalias (detector de problemas) <aluno - ônibus>
    ex.: Ônibus está há 12 minutos parado
    */
    private VelocityAnalysisDTO analyzeVehicleMovement(UUID travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Viagem não encontrada. " + travelId));

        LiveLocationDTO actuallyPosition =
                redisTrackingService.getLiveLocation(String.valueOf(travel.getId()));
        LastLocationDTO lastLocation =
                redisTrackingService.getLastLocation(String.valueOf(travel.getId()));

        VelocityAnalysisDTO result;

        // Primeiro ping
        if (lastLocation == null) {
            result = new VelocityAnalysisDTO(null, null, null, null, MovementState.INSUFFICIENT_DATA);
        }
        else {
            long elapsedSeconds = Duration
                    .between(Instant.ofEpochMilli(lastLocation.timestamp()), Instant.now())
                    .toSeconds();

            final int MIN_SECONDS = 5;
            if (elapsedSeconds < MIN_SECONDS) {
                result = new VelocityAnalysisDTO(null, null, null, null, MovementState.INSUFFICIENT_DATA);
            }
            else {
                Double distanceBetweenPings =
                        routeCalculationService.calculateHaversineDistanceInMeters(
                                actuallyPosition.longitude(), actuallyPosition.latitude(),
                                lastLocation.longitude(), lastLocation.latitude());

                final int MIN_SOLID_SPEED_DISTANCE = 5;
                if (distanceBetweenPings < MIN_SOLID_SPEED_DISTANCE) {
                    result = new VelocityAnalysisDTO(null, null, null, null, MovementState.INSUFFICIENT_DATA);
                }
                else {
                    PreviousStateDTO previousEta =
                            redisTrackingService.getPreviousEta(String.valueOf(travel.getId()));

                    if (previousEta == null || previousEta.distanceRemaining() == null) {
                        result = new VelocityAnalysisDTO(null, null, null, null, MovementState.INSUFFICIENT_DATA);
                    }
                    else {
                        double distanceRemaining = previousEta.distanceRemaining();
                        if (distanceRemaining <= 0 || distanceRemaining < distanceBetweenPings) {
                            result = new VelocityAnalysisDTO(null, null, null, null, MovementState.INSUFFICIENT_DATA);
                        }
                        else {
                            double avgSpeed = distanceBetweenPings / elapsedSeconds;
                            final double MIN_SPEED_THRESHOLD = 0.5;

                            Double newETA = null;
                            MovementState state;

                            if (avgSpeed == 0) {
                                state = MovementState.STOPPED;
                            } else if (avgSpeed <= MIN_SPEED_THRESHOLD) {
                                state = MovementState.SLOW;
                            } else {
                                state = MovementState.NORMAL;
                                if (previousEta.durationRemaining() != null) {
                                    newETA = distanceRemaining / avgSpeed;
                                    redisTrackingService.updateTripEtaState(
                                            travel.getId(),
                                            distanceRemaining,
                                            newETA,
                                            Instant.now()
                                    );
                                }
                            }

                            result = new VelocityAnalysisDTO(
                                    avgSpeed,
                                    elapsedSeconds,
                                    distanceBetweenPings,
                                    newETA,
                                    state
                            );
                        }
                    }
                }
            }
        }

        redisTrackingService.keepMemoryBetweenDriverPings(travel.getId(), actuallyPosition);

        return result;
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
