package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.InvalidNotificationStateException;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.response.NotificationStateDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RedisNotificationService {
    private final HashOperations<String, String, String> hashOperations;

    private final String HASH_KEY_PREFIX = "notification:";

    public RedisNotificationService(HashOperations<String, String, String> hashOperations) {
        this.hashOperations = hashOperations;
    }

    // read
    public NotificationStateDTO readNotificationState(UUID travelId, UUID studentId) {
        if (travelId == null || studentId == null) throw new EntityNotFoundException("Student ou Travel não encontrados");

        String key = HASH_KEY_PREFIX + travelId + ":" + studentId;

        String zone = hashOperations.get(key, "zone");
        String lastDistanceNotified = hashOperations.get(key, "lastDistanceNotified");
        String lastNotificationAt = hashOperations.get(key, "lastNotificationAt");
        String timeStamp = hashOperations.get(key, "timeStamp");

        return new NotificationStateDTO(zone, lastDistanceNotified, lastNotificationAt, timeStamp);
    }

    // verification
    public Boolean verifyNotificationState(UUID travelId, UUID studentId, Double currentDistanceMeters, NotificationStateDTO state) {
        if (state == null || state.zone().isEmpty()) return true;

        String currentZone;
        double step;

        if (currentDistanceMeters >= 1000) {
            currentZone = "FAR";
            step = 200.0;
        } else {
            currentZone = "NEAR";
            step = 30.0;
        }

        if (!currentZone.equals(state.zone())) {
            return true;
        }

        double lastDistanceNotified = Double.parseDouble(state.lastDistanceNotified());
        double distanceDelta = Math.abs(lastDistanceNotified = currentDistanceMeters);

        return distanceDelta >= step;
    }

    // update
    public void updateNotificationState(UUID travelId, UUID studentId, NotificationStateDTO newState) {
        if (travelId == null || studentId == null) throw new EntityNotFoundException("Student ou Travel não encontrados");

        String key = HASH_KEY_PREFIX + travelId + ":" + studentId;

        Map<String, String> currentState = hashOperations.entries(key);

        if (currentState.isEmpty()) {
            Map<String, String> initialState = new HashMap<>();

            initialState.put("zone", newState.zone());
            initialState.put("lastDistanceNotified", newState.lastDistanceNotified());
            initialState.put("lastNotificationAt", newState.lastNotificationAt());
            String timeStamp = String.valueOf(Instant.now());
            initialState.put("timeStamp", timeStamp);

            hashOperations.putAll(key, initialState);
            return;
        }

        Map<String, String> fieldsToUpdate = new HashMap<>();

        if (!newState.zone().equals(currentState.get("zone"))) {
            fieldsToUpdate.put("zone", newState.zone());
        }

        if (!newState.lastNotificationAt().equals(currentState.get("lastNotificationAt"))) {
            fieldsToUpdate.put("lastNotificationAt", newState.lastNotificationAt());
        }

        if (newState.lastDistanceNotified().equals(currentState.get("lastDistanceNotified"))) {
            fieldsToUpdate.put("lastDistanceNotified", newState.lastDistanceNotified());
        }

        if (!fieldsToUpdate.isEmpty()) {
            fieldsToUpdate.put("timeStamp", String.valueOf(Instant.now().toEpochMilli()));
            hashOperations.putAll(key, fieldsToUpdate);
        }

    }

}
