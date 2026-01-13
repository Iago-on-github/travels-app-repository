package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.EtaDataStatesInvalidException;
import com.travel_system.backend_app.model.dtos.VelocityAnalysisDTO;
import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.ShouldNotify;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@EnableAsync
public class AsyncNotificationService {

    private final RedisTrackingService redisTrackingService;

    public AsyncNotificationService(RedisTrackingService redisTrackingService) {
        this.redisTrackingService = redisTrackingService;
    }

    public void processNotificationType(ShouldNotify shouldNotify) {

    }

    private void slowNotification(UUID travelId, VelocityAnalysisDTO velocityAnalysis) {
        if (travelId == null || velocityAnalysis == null) throw new EtaDataStatesInvalidException("Dados da viagem inválidos ou corrompidos");

        // enviar notificação

        redisTrackingService.storeLastKnownState(String.valueOf(travelId), velocityAnalysis);
        redisTrackingService.markNotificationAsSent(String.valueOf(travelId));


    }
}
