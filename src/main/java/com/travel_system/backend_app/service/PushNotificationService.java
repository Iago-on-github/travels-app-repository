package com.travel_system.backend_app.service;

import com.travel_system.backend_app.events.NewLocationReceivedEvents;
import com.travel_system.backend_app.events.StudentProximityEvents;
import com.travel_system.backend_app.events.VehicleMovementEvents;
import com.travel_system.backend_app.exceptions.TripNotFound;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
    private final RedisTrackingService redisTrackingService;
    private final TravelRepository travelRepository;

    private final ApplicationEventPublisher eventPublisher;

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    public PushNotificationService(TravelTrackingService travelTrackingService, TravelService travelService, RouteCalculationService routeCalculationService, RedisNotificationService redisNotificationService, RedisTrackingService redisTrackingService, TravelRepository travelRepository, ApplicationEventPublisher eventPublisher) {
        this.travelTrackingService = travelTrackingService;
        this.travelService = travelService;
        this.routeCalculationService = routeCalculationService;
        this.redisNotificationService = redisNotificationService;
        this.redisTrackingService = redisTrackingService;
        this.travelRepository = travelRepository;
        this.eventPublisher = eventPublisher;
    }

    /*
        gera pushs de notificações por distância <aluno - ônibus>
        ex.: Ônibus está há 200M de você
    */
    public void checkProximityAlerts(UUID travelId, Double latitude, Double longitude) {
        LiveLocationDTO driverPosition = new LiveLocationDTO(latitude, longitude, null, 0.0, null, null);
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
                // manda evento ao rabbitMQ via event
                eventPublisher.publishEvent(new StudentProximityEvents(
                        travelId,
                        student.studentId(),
                        distance,
                        zone,
                        timestamp,
                        alertType));
                logger.info("evento publicado para a viagem: {}", travelId);

                // update no redis
                redisNotificationService.updateNotificationState(travelId, student.studentId(),
                        new NotificationStateDTO(zone,
                                distance.toString(),
                                lastNotificationAt,
                                timestamp));
            }
        });

    }

    public void processVehicleMovement(UUID travelId, Double latitude, Double longitude) {
        UUID traceId = UUID.randomUUID();
        logger.info("[Trace: {}] Iniciando processamento para viagem: {}", traceId, travelId);
        VelocityAnalysisDTO velocityAnalysis = analyzeVehicleMovement(travelId, latitude, longitude);

        ShouldNotify decision = shouldSendNotification(travelId, velocityAnalysis, traceId);

        // chama para notificação via event
        eventPublisher.publishEvent(new VehicleMovementEvents(
                travelId,
                velocityAnalysis,
                decision,
                traceId));

        redisTrackingService.storeLastKnownState(String.valueOf(travelId), velocityAnalysis);
    }

    // usa analyzeVehicleMovement e decide se deve notificar
    private ShouldNotify shouldSendNotification(UUID travelId, VelocityAnalysisDTO velocityAnalysis, UUID traceId) {
        // verificar mudanças de estado
        AnalyzeMovementStateDTO lastMovementState = redisTrackingService.getLastMovementState(String.valueOf(travelId));
        MovementState actualMovementState = velocityAnalysis.movementState();

        // primeiro ciclo: não notificar
        if (lastMovementState == null || lastMovementState.movementState() == null || lastMovementState.stateStartedAt() == null || lastMovementState.lastNotificationSendAt() == null) {
            logger.info("[Trace: {}] Decisão: Não notificar", traceId);
            return ShouldNotify.SHOULD_NO_NOTIFY;
        }

        Instant now = Instant.now();

        final long STATE_TIME_LIMIT_MS = 4_000;
        final long NOTIFICATION_COOLDOWN_MS = 12_000;
        final long NOTIFICATION_COOLDOWN_MS_STOPPED = 300_000;

        // comparar estados
        // se o estado mudou, ainda nao notifica
        if (!actualMovementState.equals(lastMovementState.movementState())) {
            return ShouldNotify.SHOULD_NO_NOTIFY;
        }

        if (actualMovementState.equals(MovementState.NORMAL)) {
            return ShouldNotify.SHOULD_NO_NOTIFY;
        }

        long durationOnState = now.toEpochMilli() - lastMovementState.stateStartedAt().toEpochMilli();
        long timeSinceLastNotification = now.toEpochMilli() - lastMovementState.lastEtaNotificationAt().toEpochMilli();

        boolean stayedLongEnough = durationOnState >= STATE_TIME_LIMIT_MS;
        boolean cooldownExpired = timeSinceLastNotification >= NOTIFICATION_COOLDOWN_MS;

        if (stayedLongEnough && cooldownExpired) {
            if (actualMovementState.equals(MovementState.SLOW)) {
                logger.info("[Trace: {}] Decisão: SHOULD_NOTIFY_SLOW", traceId);
                return ShouldNotify.SHOULD_NOTIFY_SLOW;
            }
            if (actualMovementState.equals(MovementState.STOPPED) && timeSinceLastNotification >= NOTIFICATION_COOLDOWN_MS_STOPPED) {
                logger.info("[Trace: {}] Decisão: NOTIFY_STOPPED", traceId);
                return ShouldNotify.SHOULD_NOTIFY_STOPPED;
            }
        }
        logger.info("[Trace: {}] Decisão: NO_NOTIFY (Motivo: Cooldown/Tempo de estado não atingido)", traceId);
        return ShouldNotify.SHOULD_NO_NOTIFY;
    }

    /*
    gera pushs de notificações por anomalias (detector de problemas) <aluno - ônibus>
    ex.: Ônibus está há 12 minutos parado
    */
    private VelocityAnalysisDTO analyzeVehicleMovement(UUID travelId, Double latitude, Double longitude) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Viagem não encontrada. " + travelId));

        LiveLocationDTO lastRecentPosition = redisTrackingService.getLiveLocation(String.valueOf(travel.getId()));
        LastLocationDTO lastLocation = redisTrackingService.getLastLocation(String.valueOf(travel.getId()));

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
                                longitude, latitude,
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

        LiveLocationDTO actuallyPosition = getLiveLocationDTO(latitude, longitude, lastRecentPosition);

        redisTrackingService.keepMemoryBetweenDriverPings(travel.getId(), actuallyPosition);

        return result;
    }

    private LiveLocationDTO getLiveLocationDTO(Double latitude, Double longitude, LiveLocationDTO lastRecentPosition) {
        String geometry = lastRecentPosition != null ? lastRecentPosition.geometry() : null;
        double distance = lastRecentPosition != null ? lastRecentPosition.distance() : 0.0;
        double lastCalcLat = lastRecentPosition != null ? lastRecentPosition.lastCalcLat() : 0.0;
        double lastCalcLng = lastRecentPosition != null ? lastRecentPosition.lastCalcLng() : 0.0;

        return new LiveLocationDTO(
                latitude,
                longitude,
                geometry,
                distance,
                lastCalcLat,
                lastCalcLng);
    }

    // distance between driver and student
    protected List<DistanceResponseDTO> distanceBetweenPositions(UUID travelId, LiveLocationDTO driverPosition) {
        Set<StudentTravelResponseDTO> linkedStudentTravel = travelService.linkedStudentTravel(travelId);

        logger.info("Viagem {}: Iniciando cálculo de distância para {} alunos vinculados.", travelId, linkedStudentTravel.size());

        List<DistanceResponseDTO> results = linkedStudentTravel.stream()
                .filter(student -> {
                    boolean hasPosition = student.position() != null;
                    if (!hasPosition) {
                        logger.warn("Aluno {} ignorado: Posição (GeoPosition) está nula no banco.", student.studentId());
                    }
                    return hasPosition;
                })
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
        logger.info("Viagem {}: Cálculo concluído. {} alunos processados com sucesso.", travelId, results.size());
        return results;
    }
}
