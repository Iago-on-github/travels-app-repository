package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import com.travel_system.backend_app.exceptions.TripNotFound;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
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
    public void storeLiveLocation(String travelId, String latitude, String longitude, String distance, String geometry) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        Map<String, String> data = new HashMap<>();

        // dados cache
        data.put("distance", distance);
        data.put("geometry", geometry);

        // ponto de referência de onde a rota foi calculada
        data.put("last_calc_lat", latitude);
        data.put("last_calc_lng", longitude);

        // obtem o timestamp real do servidor (ultimo ping)
        String currentTimeStamp = String.valueOf(Instant.now().toEpochMilli());
        data.put("timestamp", currentTimeStamp);

        hashOperations.putAll(key, data);
    }

    // retorna o último ETA armazenado + a distância
    public PreviousStateDTO getPreviousEta(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        String durationRemaining = hashOperations.get(key, "durationRemaining");
        String distance = hashOperations.get(key, "distance");
        String timeStampLastPing = hashOperations.get(key, "timestamp");

        return new PreviousStateDTO(Double.parseDouble(durationRemaining), Double.parseDouble(distance), Long.parseLong(timeStampLastPing));
    }

    // fornece a loc mais recente e o timestamp para o front-end
    public LiveLocationDTO getLiveLocation(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        Map<String, String> data = hashOperations.entries(key);

        if (data == null || data.isEmpty()) {
            throw new TripNotFound("Dados de rastramento em tempo real não encontrados");
        }

        // última posição
        String latitude = data.get("lat");
        String longitude = data.get("lng");

        // dados de rota que ficarão em cache
        String geometry = data.get("geometry");
        String distance = data.get("distance");

        // posição do último cálculo da chamada da api
        String lastCalcLat = data.get("last_calc_lat");
        String lastCalcLng = data.get("last_calc_lng");

        if (latitude == null || longitude == null) {
            throw new NoSuchCoordinates("Latitude/Longitude atuais não encontradas");
        }

        try {
            return new LiveLocationDTO(
                    Double.parseDouble(latitude),
                    Double.parseDouble(longitude),
                    geometry,
                    distance != null ? Double.parseDouble(distance) : 0.0,
                    lastCalcLat != null ? Double.parseDouble(lastCalcLat) : null,
                    lastCalcLng != null ? Double.parseDouble(lastCalcLng) : null);
        } catch (NumberFormatException e) {
            throw new NoSuchCoordinates("Dados de coordenadas corrompidos ou inválidos. " + e.getMessage());
        }
    }

    // atualiza ETA restante, distância restante e o status atualizado
    public void storeTravelMetadata(String travelId, String durationRemaining, String distance, String status) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        // HSET: vai atualizar os campos de distance, eta e status sem afetar LAT/LNG
        hashOperations.put(key, "durationRemaining", durationRemaining);
        hashOperations.put(key, "timestamp", String.valueOf(Instant.now().toEpochMilli()));
        hashOperations.put(key, "distance", distance);
        hashOperations.put(key, "status", status);
    }

    // chamada dentro de endtravel para remover todos os dados do redis.
    public void deleteTrackingData(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        redisTemplate.delete(key);
    }
}
