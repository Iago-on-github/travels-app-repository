package com.travel_system.backend_app.utils;

import com.travel_system.backend_app.exceptions.DomainValidationException;
import com.travel_system.backend_app.exceptions.InvalidDeviceTokenException;
import com.travel_system.backend_app.model.DeviceToken;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.enums.Platform;
import com.travel_system.backend_app.repository.DeviceTokenRepository;
import com.travel_system.backend_app.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FirebaseNotificationSender {

    private final DeviceTokenRepository deviceTokenRepository;
    private final StudentRepository studentRepository;

    public FirebaseNotificationSender(DeviceTokenRepository deviceTokenRepository, StudentRepository studentRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.studentRepository = studentRepository;
    }

    // proximos passos:
    // buscar todos os tokens ativos do student
    // enviar notificação para cada um
    // trtar falhas do firebase (token inválido: active = false)
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
}
