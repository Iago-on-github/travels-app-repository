package com.travel_system.backend_app.utils;

import com.google.firebase.messaging.*;
import com.travel_system.backend_app.exceptions.DomainValidationException;
import com.travel_system.backend_app.model.DeviceToken;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.dtos.MovementNotificationEventDTO;
import com.travel_system.backend_app.model.dtos.StudentTokensDTO;
import com.travel_system.backend_app.model.dtos.VehicleMovementNotificationDTO;
import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.Platform;
import com.travel_system.backend_app.model.enums.Priority;
import com.travel_system.backend_app.repository.DeviceTokenRepository;
import com.travel_system.backend_app.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;
import static java.util.stream.Collectors.toList;

@Service
public class FirebaseNotificationSender {

    private final DeviceTokenRepository deviceTokenRepository;
    private final StudentRepository studentRepository;
    private final FirebaseMessaging firebaseMessaging;
    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationSender.class);

    public FirebaseNotificationSender(DeviceTokenRepository deviceTokenRepository, StudentRepository studentRepository, FirebaseMessaging firebaseMessaging) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.studentRepository = studentRepository;
        this.firebaseMessaging = firebaseMessaging;
    }

    // registra/atualiza os tokens do usuário
    public void manageUserToken(Student student, String token, Platform platform) {
        if (student == null || token == null || token.isBlank() || platform == null) throw new DomainValidationException("Token is null");

        Optional<DeviceToken> existingDeviceToken = deviceTokenRepository.findDeviceTokenByToken(token);

        DeviceToken deviceToken;
        if (existingDeviceToken.isPresent()) {
            deviceToken = existingDeviceToken.get();

            deviceToken.setStudent(student);
        } else {
            deviceToken = new DeviceToken();

            deviceToken.setStudent(student);
            deviceToken.setToken(token.trim());
        }
        deviceToken.setPlatform(platform);
        deviceTokenRepository.save(deviceToken);
    }

    // enviar notificação ao firebase
    public VehicleMovementNotificationDTO pushNotificationToFirebase(MovementNotificationEventDTO movementNotificationEvent) {
        UUID studentId = movementNotificationEvent.studentId();
        UUID travelId = movementNotificationEvent.travelId();
        UUID traceId = movementNotificationEvent.traceId();
        MovementState movementState = movementNotificationEvent.movementState();
        Priority priority = movementNotificationEvent.priority();
        String message = movementNotificationEvent.message();

        Set<String> studentActiveTokens = studentActiveTokens(studentId);

        if (studentActiveTokens.isEmpty()) {
            logger.info("Nenhum token ativo para o aluno {}, pulando notificação.", studentId);
            return null;
        }

        List<String> deviceTokens = studentActiveTokens.stream().toList();

        MulticastMessage payload = convertMovementNotifyToFcmFormat(travelId, movementState, priority, message, deviceTokens);

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(payload);

            logger.info("[Trace: {}] Tokens enviados ao firebase: {}", traceId, response.getSuccessCount());
            if (response.getFailureCount() > 0) {
                List<String> failureTokens = getFailureDeviceTokens(response, deviceTokens);
                logger.error("Falha crítica no FCM para o aluno: {} {}", studentId, response.getFailureCount());
                if (!failureTokens.isEmpty()) {
                    logger.warn("Desativando {} tokens inválidos no banco.", failureTokens.size());
                    deviceTokenRepository.deactivateTokensByValue(failureTokens);
                }
            }
        } catch (FirebaseMessagingException e) {
            logger.error("Erro no envio da mensagem para o Firebase: {} {}", e.getMessagingErrorCode(), traceId);
        }

        return new VehicleMovementNotificationDTO(travelId, movementState, Instant.now(), message, priority);
    }

    // retorna os tokens que falharam da response
    private static List<String> getFailureDeviceTokens(BatchResponse response, List<String> deviceTokens) {
        List<SendResponse> responses = response.getResponses();
        List<String> failureTokens = new ArrayList<>();

        for (int i = 0; i < responses.size(); i++) {
            if (!responses.get(i).isSuccessful()) {

                String failedToken = deviceTokens.get(i);

                MessagingErrorCode messagingErrorCode = responses.get(i).getException()
                        .getMessagingErrorCode();

                // usuário removeu o app ou limpou os dados ou formato incorreto do token
                if (messagingErrorCode.equals(MessagingErrorCode.UNREGISTERED) || messagingErrorCode.equals(MessagingErrorCode.INVALID_ARGUMENT)) {
                    logger.info("Processo de desativação do token... motivo: {}", messagingErrorCode);

                    // lista temporária para desativar os tokens
                    failureTokens.add(failedToken);
                }

                if (messagingErrorCode.equals(MessagingErrorCode.QUOTA_EXCEEDED)) {
                    logger.warn("Limite do firebase atingido: {}", messagingErrorCode);
                }
            }
        }
        return failureTokens;
    }

    // converte dto para formato fcm
    private MulticastMessage convertMovementNotifyToFcmFormat(UUID travelId, MovementState movementState, Priority priority, String message, List<String> studentActiveTokens) {
        Map<String, String> data = new HashMap<>();

        data.put("travelId", String.valueOf(travelId));
        data.put("movementState", String.valueOf(movementState));
        data.put("priority", String.valueOf(priority));
        data.put("message", message);

        Set<String> convertedTokens = new HashSet<>(studentActiveTokens);

        return MulticastMessage.builder()
                .putAllData(data)
                .addAllTokens(convertedTokens)
                .build();
    }

    // pega todos os tokens ativos do usuário
    private Set<String> studentActiveTokens(UUID studentId) {
        return studentRepository.findActiveTokensByStudentId(studentId);
    }
}
