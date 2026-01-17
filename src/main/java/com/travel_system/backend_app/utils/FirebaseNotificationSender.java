package com.travel_system.backend_app.utils;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.travel_system.backend_app.exceptions.DeviceTokenNotFoundException;
import com.travel_system.backend_app.exceptions.DomainValidationException;
import com.travel_system.backend_app.exceptions.InvalidDeviceTokenException;
import com.travel_system.backend_app.model.DeviceToken;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.VehicleMovementNotificationDTO;
import com.travel_system.backend_app.model.enums.MovementState;
import com.travel_system.backend_app.model.enums.Platform;
import com.travel_system.backend_app.model.enums.Priority;
import com.travel_system.backend_app.repository.DeviceTokenRepository;
import com.travel_system.backend_app.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FirebaseNotificationSender {

    private final DeviceTokenRepository deviceTokenRepository;
    private final StudentRepository studentRepository;
    private final FirebaseMessaging firebaseMessaging;

    public FirebaseNotificationSender(DeviceTokenRepository deviceTokenRepository, StudentRepository studentRepository, FirebaseMessaging firebaseMessaging) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.studentRepository = studentRepository;
        this.firebaseMessaging = firebaseMessaging;
    }

    // proximos passos:
    // buscar todos os tokens ativos do student
    // enviar notificação para cada um
    // trtar falhas do firebase (token inválido: active = false)

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

    // pega todos os tokens ativos do usuário
    public Set<DeviceToken> studentActiveTokens(UUID studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("estudante não encontrado" + studentId));

        return student.getDeviceTokens()
                .stream().filter(DeviceToken::isActive).collect(Collectors.toSet());
    }

    // enviar notificação ao firebase
    public VehicleMovementNotificationDTO pushNotificationToFirebase(UUID studentId, UUID travelId, MovementState movementState, Priority priority, String message) {
        Set<DeviceToken> studentActiveTokens = studentActiveTokens(studentId);

        if (studentActiveTokens.isEmpty()) return null;

        VehicleMovementNotificationDTO movementNotificationDTO = new VehicleMovementNotificationDTO(travelId, movementState, Instant.now(), message, priority);

        Message payload =;

        studentActiveTokens.forEach(deviceToken -> {
            firebaseMessaging.send(payload);
        });
    }

    // tratar falhas
}
