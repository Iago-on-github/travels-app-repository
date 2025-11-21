package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.dtos.mapboxApi.PreviousStateDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;


@Service
public class RedisTrackingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, String> hashOperations;
    private final String HASH_KEY_PREFIX = "travelId:";

    public RedisTrackingService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }

    // armazena a localização mais recente do motorista em cache com redisTemplate
    public void storeLiveLocation(String travelId, String currentLat, String currentLng, String durationRemaining) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) {
            throw new RuntimeException("travelId não informado, " + travelId);
        }

        // obtem o timestamp real do servidor (ultimo ping)
        String currentTimeStamp = String.valueOf(Instant.now().toEpochMilli());

        Map<String, String> data = new HashMap<>();
        data.put("lat", currentLat);
        data.put("lng", currentLng);
        data.put("durationRemaining", durationRemaining);

        data.put("timestamp", currentTimeStamp);

        hashOperations.putAll(key, data);
    }

    // retorna o último ETA armazenado + a distância
    public PreviousStateDTO getPreviousEta(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        String durationRemaining = hashOperations.get(key, "durationRemaining");
        String distance = hashOperations.get(key, "distance");
        String timeStampLastPing = hashOperations.get(key, "timestamp");

        return new PreviousStateDTO(Double.parseDouble(durationRemaining), Double.parseDouble(distance), Long.parseLong(timeStampLastPing));
    }
    // fornece a loc mais recente e o timestamp para o front-end
    public Map<String, String> getLiveLocation(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new RuntimeException("TravelId is null, " + travelId);

        return hashOperations.entries(key);
    }

    // atualiza ETA restante, distância restante e o status atualizado
    public void storeTravelMetadata(String travelId, String durationRemaining, String distance, String status) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new RuntimeException("TravelId is null, " + travelId);

        // HSET: vai atualizar os campos de distance, eta e status sem afetar LAT/LNG
        hashOperations.put(key, "durationRemaining", durationRemaining);
        hashOperations.put(key, "timestamp", String.valueOf(Instant.now().toEpochMilli()));
        hashOperations.put(key, "distance", distance);
        hashOperations.put(key, "status", status);
    }

    // chamada dentro de endtravel para remover todos os dados do redis.
    public void deleteTrackingData(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new RuntimeException("TravelId is null, " + travelId);

        redisTemplate.delete(key);
    }
}
