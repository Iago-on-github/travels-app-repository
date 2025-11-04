package com.travel_system.backend_app.service;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
    public void storeLiveLocation(String travelId, String currentLat, String currentLng, String timeStamp) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) {
            throw new RuntimeException("travelId não informado, " + travelId);
        }

        Map<String, String> data = new HashMap<>();
        data.put("lat", currentLat);
        data.put("lng", currentLng);
        data.put("timestamp", timeStamp);

        hashOperations.putAll(key, data);
    }

    // fornece a loc mais recente e o timestamp para o front-end
    public Map<String, String> getLiveLocation(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new RuntimeException("TravelId is null, " + travelId);

        return hashOperations.entries(key);
    }

    // atualiza ETA restante, distância restente e o status atualizado
    public void storeTravelMetadata(String travelId, String eta, String distance, String status) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new RuntimeException("TravelId is null, " + travelId);

        // HSET: vai atualizar os campos de distance, eta e status sem afetar LAT/LNG
        hashOperations.put(key, "eta", eta);
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
