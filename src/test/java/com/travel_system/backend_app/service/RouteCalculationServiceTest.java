package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    @InjectMocks
    private RouteCalculationService routeCalculationService;

    @Mock
    private PolylineService polylineService;

    @Nested
    class isRouteDeviation {
        @DisplayName("Deve verificar se a localização do motorista está muito próximo à rota padrão (isRouteOff = false)")
        @Test
        void shouldCheckIfRouteDeviationWhenOnRoute() {
            final double currentLat = -22.0;
            final double currentLng = -47.0;
            final String polyline = "simulated_route";

            List<Point> mockPoints = List.of(
                    Point.fromLngLat(-47.1, -22.1),
                    Point.fromLngLat(currentLng, currentLat)
            );

            doReturn(mockPoints)
                    .when(polylineService)
                    .formattedPolyline(anyString());

            RouteDeviationDTO result = routeCalculationService.isRouteDeviation(currentLat, currentLng, polyline);

            assertFalse(result.isOffRoute());
        }

        @DisplayName("Deve verificar se a localização do motorista está distante à rota padrão (isRouteOff = true)")
        @Test
        void shouldCheckIfRouteDeviationWhenOffRoute() {
            final double currentLat = -21.9991;
            final double currentLng = -47.0500;
            final String polyline = "simulated_route";

            List<Point> mockPoints = List.of(
                    Point.fromLngLat(-47.0000, -22.0000),
                    Point.fromLngLat(-47.1000, -22.0000)
            );

            doReturn(mockPoints)
                    .when(polylineService)
                    .formattedPolyline(anyString());

            RouteDeviationDTO result = routeCalculationService.isRouteDeviation(currentLat, currentLng, polyline);

            assertTrue(result.isOffRoute());
            assertTrue(result.distanceToRouteMeters() > 50.0);
        }

        @DisplayName("Deve lançar exceção quando a Polyline ou as coordenadas não forem fornecidas corretamente")
        @Test
        void throwExceptionWhenNoSuchPolylineOrCoordinates() {
            final Double currentLat = -21.9991;
            final Double currentLng = null;
            final String polyline = "polyline_teste";

            assertThrows(NoSuchCoordinates.class, () -> {
                routeCalculationService.isRouteDeviation(currentLat, currentLng, polyline);
            });

            verify(polylineService, never()).formattedPolyline(polyline);
        }

        @DisplayName("Deve verificar se o tamanho do Polyline é menor que dois")
        @Test
        void shouldCheckPolylineSizeIsLessThanTwo() {
            final double currentLat = -21.9991;
            final double currentLng = -47.0500;
            final String polyline = "xasda23";

            doReturn(Collections.emptyList())
                    .when(polylineService)
                    .formattedPolyline(polyline);

            RouteDeviationDTO result = routeCalculationService.isRouteDeviation(currentLat, currentLng, polyline);

            assertFalse(result.isOffRoute());
            assertEquals(0, result.distanceToRouteMeters());
        }
    }
}