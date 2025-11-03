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

    private RedisTrackingService redisTrackingService;

    private final String travelId = UUID.randomUUID().toString();
    private final String currentLat = "40.7128";
    private final String currentLng = "-74.0060";
    private final String timeStamp = "1700000000";
    private final String HASH_KEY_PREFIX = "travelId:";
    private final String expectedRedisKey = HASH_KEY_PREFIX + travelId;

    @BeforeEach
    void setUp() {
        RedisTemplate templateMock = Mockito.mock(RedisTemplate.class);

        when(templateMock.opsForHash()).thenReturn(hashOperations);

        this.redisTrackingService = new RedisTrackingService(templateMock);
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

    @Test
    void getLiveLocation() {
    }

    @Test
    void storeTravelMetadata() {
    }

    @Test
    void deleteTrackingData() {
    }
}