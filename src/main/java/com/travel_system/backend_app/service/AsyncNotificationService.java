package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.EtaDataStatesInvalidException;
import com.travel_system.backend_app.model.dtos.VehicleMovementNotificationDTO;
import com.travel_system.backend_app.model.dtos.VelocityAnalysisDTO;
import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.Priority;
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

    /*
    * envia notificação quando o ônibus estiver LENTO (slow)
    * deve marcar corretamente no redis quando a notificação foi enviada
    * */
    // posteriormente validar alinhamento com métricas e logs para produção

    // já inserido e configurado o firebase no projeto. Verificar os próximos passos
    private void slowNotification(UUID travelId, VelocityAnalysisDTO velocityAnalysis) {
        if (travelId == null || velocityAnalysis == null) throw new EtaDataStatesInvalidException("Dados da viagem inválidos ou corrompidos");

        if (!velocityAnalysis.movementState().equals(MovementState.SLOW)) return;

        // recuperar infos e preparar dto de envio ao firebase
        MovementState movementState = velocityAnalysis.movementState();
        Instant now = Instant.now();
        final String message = "Alerta de ônibus lento. Fique atento.";
        Priority priority = Priority.HIGH;

        VehicleMovementNotificationDTO movementNotification = new VehicleMovementNotificationDTO(travelId, movementState, now, message, priority);

        // enviar notificação

        // controlar cooldawn
        redisTrackingService.markNotificationAsSent(String.valueOf(travelId));

    }
}
