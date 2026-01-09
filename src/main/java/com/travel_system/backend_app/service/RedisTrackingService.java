package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.PreviousStateDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.response.LastLocationDTO;
import com.travel_system.backend_app.repository.DriverRepository;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


@Service
public class RedisTrackingService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HashOperations<String, String, String> hashOperations;
    private final TravelRepository travelRepository;
    private final String HASH_KEY_PREFIX = "travelId:";

    public RedisTrackingService(RedisTemplate<String, Object> redisTemplate, TravelRepository travelRepository) {
        this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
        this.travelRepository = travelRepository;
    }

    // armazena a localização mais recente do motorista em cache com redisTemplate
    public void storeLiveLocation(String travelId, String latitude, String longitude, String distance, String geometry) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        Map<String, String> data = new HashMap<>();

        // dados cache
        data.put("distanceRemaining", distance);
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
        Travel travel = travelRepository.findById(UUID.fromString(travelId))
                .orElseThrow(() -> new TripNotFound("Viagem não encontrada: " + travelId));

        String key = HASH_KEY_PREFIX + travel.getId();

        if (travel.getId() == null) throw new TripNotFound("Id da viagem não encontrado " + travel.getId());

        String durationRemaining = hashOperations.get(key, "durationRemaining");
        String distance = hashOperations.get(key, "distanceRemaining");
        String timestampLastPing = hashOperations.get(key, "timestamp");

        return new PreviousStateDTO(
                durationRemaining != null ? Double.parseDouble(durationRemaining) : null,
                distance != null ? Double.parseDouble(distance) : null,
                timestampLastPing != null ? Long.parseLong(timestampLastPing) : null);
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

    // fornece a última loc registrada (antes da loc mais recente)
    public LastLocationDTO getLastLocation(UUID travelId) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Viagem não encontrada: " + travelId));

        String key = HASH_KEY_PREFIX + travel.getId();

        // read hash
        String lastPingLat = hashOperations.get(key, "last_ping_lat");
        String lastPingLng = hashOperations.get(key, "last_ping_lng");
        String timestamp = hashOperations.get(key, "timestamp");

        // is first ping return null. Who calling this method decided create an initial state
        if (timestamp == null) return null;
        else {
            if (lastPingLat == null || lastPingLng == null || timestamp.isEmpty()) {
                throw new NoFoundPositionException("Últimos dados de latitude, longitude ou timestamp inválidos ou corrompidos");
            }

            double LastPingLatToDouble = Double.parseDouble(lastPingLat);
            double LastPingLngToDouble = Double.parseDouble(lastPingLng);
            long timestampToLong = Long.parseLong(timestamp);

            return new LastLocationDTO(LastPingLatToDouble, LastPingLngToDouble, timestampToLong);

        }
    }

    // atualiza ETA restante, distância restante e o status atualizado
    public void storeTravelMetadata(String travelId, String durationRemaining, String distance, String status) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        // HSET: vai atualizar os campos de distance, eta e status sem afetar LAT/LNG
        hashOperations.put(key, "durationRemaining", durationRemaining);
        hashOperations.put(key, "timestamp", String.valueOf(Instant.now().toEpochMilli()));
        hashOperations.put(key, "distanceRemaining", distance);
        hashOperations.put(key, "status", status);
    }

    // chamada dentro de endtravel para remover todos os dados do redis.
    public void deleteTrackingData(String travelId) {
        String key = HASH_KEY_PREFIX + travelId;

        if (travelId == null) throw new TripNotFound("Id da viagem não encontrado " + travelId);

        redisTemplate.delete(key);
    }

    // mantém memória entre os pings do driver
    public void keepMemoryBetweenDriverPings(UUID travelId, LiveLocationDTO driverPosition) {
        Travel travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new TripNotFound("Viagem não encontrada: " + travelId));
        String now = String.valueOf(Instant.now());

        String key = HASH_KEY_PREFIX + travel.getId();

        Map<String, String> data = hashOperations.entries(key);

        // read stats
        String lastPingTimestamp = data.get("timestamp");

//        String lastPingLat = data.get("last_ping_lat");
//        String lastPingLng = data.get("last_ping_lng");

        // if without state
        if (lastPingTimestamp == null) {
            data.put("last_ping_lat", String.valueOf(driverPosition.latitude()));
            data.put("last_ping_lng", String.valueOf(driverPosition.longitude()));
            data.put("timestamp", now);

            hashOperations.putAll(key, data);
        } else {
            data.put("last_ping_lat", String.valueOf(driverPosition.latitude()));
            data.put("last_ping_lng", String.valueOf(driverPosition.longitude()));
            data.put("timestamp", now);

            hashOperations.putAll(key, data);
        }
    }

    // atualiza o estado de ETA da viagem
    public void updateTripEtaState(UUID travelId, Double distanceRemaining, Double durationRemaining, Instant timestamp) {
        if (travelId == null || distanceRemaining == null || durationRemaining == null || timestamp == null) {
            throw new EtaDataStatesInvalidException("Dados do estado ETA inválidos ou corrompidos");
        }

        String distanceRemainingString = String.valueOf(distanceRemaining);
        String durationRemainingToString = String.valueOf(durationRemaining);
        String timestampToString = String.valueOf(timestamp.toEpochMilli());

        String key = HASH_KEY_PREFIX + travelId;

        Map<String, String> data = new HashMap<>();

        data.put("distanceRemaining", distanceRemainingString);
        data.put("durationRemaining", durationRemainingToString);
        data.put("timestamp", timestampToString);

        hashOperations.putAll(key, data);
    }
}
