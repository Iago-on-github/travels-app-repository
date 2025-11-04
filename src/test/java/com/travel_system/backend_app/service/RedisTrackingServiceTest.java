package com.travel_system.backend_app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisTrackingServiceTest {

    /*
    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */
    private RedisTrackingService redisTrackingService;

    private final String travelId = UUID.randomUUID().toString();
    private final String currentLat = "40.7128";
    private final String currentLng = "-74.0060";
    private final String timeStamp = "1700000000";
    private final String HASH_KEY_PREFIX = "travelId:";
    private final String expectedRedisKey = HASH_KEY_PREFIX + travelId;

    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);

        // faz casting explícito para evitar erro com Generics
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOperations);

        this.redisTrackingService = new RedisTrackingService(redisTemplate);
    }

    @Mock
    private HashOperations<String, String, String> hashOperations;

    @DisplayName("It should store the current position of the driver in the cache")
    @Test
    void storeLiveLocationSuccessfully() {
        redisTrackingService.storeLiveLocation(travelId, currentLat, currentLng, timeStamp);

        Map<String, String> expectedData = new HashMap<>();
        expectedData.put("lat", currentLat);
        expectedData.put("lng", currentLng);
        expectedData.put("timestamp", timeStamp);

        verify(hashOperations).putAll(
                eq(expectedRedisKey),
                eq(expectedData)
        );
    }

    @DisplayName("It throw exception and not call redis when travel is null")
    @Test
    void shouldThrowExceptionAndNotCallRedisWhenTravelIdIsNull() {
        assertThrows(RuntimeException.class, () -> {
            redisTrackingService.storeLiveLocation(null, currentLat, currentLng, timeStamp);
        });

        verify(hashOperations, never()).putAll(any(), any());
    }

    @DisplayName("It should return the current position and timestamp from the front-end")
    @Test
    void getLiveLocationWithSuccess() {
        redisTrackingService.getLiveLocation(travelId);

        verify(hashOperations).entries(
                eq(expectedRedisKey)
        );
    }

    @DisplayName("It throw exception when key is null")
    @Test
    void shouldThrowExceptionWhenKeyIsNull() {
        assertThrows(RuntimeException.class, () -> {
            redisTrackingService.getLiveLocation(null);
                });

        verify(hashOperations, never()).entries(any());
    }

    @DisplayName("It should update the ETA and the status must be used individually by the HSET")
    @Test
    void shouldStoreTravelMetadataSuccessfully() {
        String eta = "300s";
        String distance = "5.2km";
        String status = "IN_TRANSIT";

        redisTrackingService.storeTravelMetadata(travelId, eta, distance, status);

        verify(hashOperations).put(eq(expectedRedisKey), eq("eta"), eq(eta));
        verify(hashOperations).put(eq(expectedRedisKey), eq("distance"), eq(distance));
        verify(hashOperations).put(eq(expectedRedisKey), eq("status"), eq(status));
    }

    @DisplayName("Should throw exception and not call Redis if travelId is NULL")
    @Test
    void shouldThrowExceptionWhenInstoreTravelMetadataKeyIsNull() {
        assertThrows(RuntimeException.class, () -> {
            redisTrackingService.storeTravelMetadata(null, null, null, null);
        });

        verify(hashOperations, never()).put(any(), any(), any());
    }

    @DisplayName("Should remove all Redis data with successfully")
    @Test
    void deleteTrackingDataWithSuccess() {
        redisTrackingService.deleteTrackingData(travelId);

        verify(redisTemplate).delete(eq(expectedRedisKey));
    }

    @DisplayName("Should throw exception when travelId is NULL")
    @Test
    void shouldThrowExceptionWhenTravelIdOfDeleteTrackingDataIsNull() {
        assertThrows(RuntimeException.class, () -> {
            redisTrackingService.deleteTrackingData(null);
        });

        verify(redisTemplate, never()).delete(eq(expectedRedisKey));
    }
}