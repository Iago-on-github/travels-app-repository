package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.RecalculateEtaException;
import com.travel_system.backend_app.exceptions.TravelException;
import com.travel_system.backend_app.exceptions.TravelStudentAssociationNotFoundException;
import com.travel_system.backend_app.exceptions.TripNotFound;
import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.PreviousStateDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import com.travel_system.backend_app.model.enums.TravelStatus;
import com.travel_system.backend_app.repository.TravelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelTrackingServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======

    - Os testes de cada method devem ser isolados por classes anotadas com @nested
    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.
    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer
    - Em cenários de Success, os métodos devem conter "With Success" nos seus respetivos nomes.
    - Em cenários de Error, os métodos devem conter "throwExceptionWhen…" nos seus respetivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private TravelTrackingService travelTrackingService;

    @Mock
    private TravelRepository travelRepository;

    @Mock
    private RouteCalculationService routeCalculationService;

    @Mock
    private RedisTrackingService redisTrackingService;

    @Mock
    private MapboxAPIService mapboxAPIService;

    @Mock
    private Clock clock;

    private Travel travel;

    private RouteDeviationDTO routeDeviationDTO;
    private RouteDeviationDTO routeDeviationDTORouteOff;
    private RouteDetailsDTO routeDetailsDTO;
    private PreviousStateDTO previousEta;

    final long INITIAL_TIME = 1000000000000L;
    final double PREVIOUS_DURATION = 180.0;
    final double EXPECTED_DURATION = 120.0;

    @Captor
    private ArgumentCaptor<String> durationCaptor;
    @Captor
    private ArgumentCaptor<String> polylineCaptor;

    @BeforeEach
    void setUp() {

        routeDetailsDTO = new RouteDetailsDTO(53231.0, 8638123.2, "any_geometry");

        routeDeviationDTO = new RouteDeviationDTO(39.4, true, 2123.3, 12434.2);
        routeDeviationDTORouteOff = new RouteDeviationDTO(39.4, false, 2123.3, 12434.2);

        previousEta = new PreviousStateDTO(18726.1, 28468753.2, 7198728L);

        travel = createTravelEntity(
                UUID.randomUUID(),
                TravelStatus.PENDING,
                null,
                Collections.emptySet(),
                Instant.now(),
                Instant.now().plusMillis(1000000),
                "encoded_route_simulada_xyz123",
                1250.5,
                15500.2,
                -23.5505,
                -46.6333,
                -22.9068,
                -43.1729
        );
    }

    @Nested
    class processNewLocation {

        @DisplayName("Deve processar a nova localização com sucesso quando o motorista está fora da rota")
        @Test
        void shouldProcessNewLocationWithSuccessWhenIsRouteOff() {
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            Double currentLng = -21.832;
            Double currentLat = -11.321;

            when(travelRepository.findById(any(UUID.class))).thenReturn(Optional.of(travel));

            when(routeCalculationService.isRouteDeviation(currentLat, currentLng, travel.getPolylineRoute()))
                    .thenReturn(routeDeviationDTO);

            when(mapboxAPIService.recalculateETA(currentLng, currentLat, travel.getFinalLatitude(), travel.getFinalLongitude()))
                    .thenReturn(routeDetailsDTO);

            travelTrackingService.processNewLocation(travel.getId(), currentLat, currentLng);

//            verify(redisTrackingService).storeLiveLocation(
//                    eq(travel.getId().toString()),
//                    anyString(),
//                    anyString(),
//                    durationCaptor.capture()
//            );

            verify(redisTrackingService).storeTravelMetadata(
                    eq(travel.getId().toString()),
                    polylineCaptor.capture(),
                    eq(routeDetailsDTO.distance().toString()),
                    eq(travel.getTravelStatus().toString())
            );

            assertEquals(routeDetailsDTO.duration().toString(), durationCaptor.getValue());
            assertEquals(routeDetailsDTO.geometry(), polylineCaptor.getValue());

            verify(redisTrackingService, never()).getPreviousEta(anyString());

        }

        @DisplayName("Deve processar a nova localização com sucesso quando o motorista está na rota padrão")
        @Test
        void shouldProcessNewLocationWithSuccessWhenIsNotRouteOff() {
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            Double currentLng = -21.832;
            Double currentLat = -11.321;

            Double currentDistance = travel.getDistance();
            String currentPolyline = travel.getPolylineRoute();

            RouteDeviationDTO deviation = new RouteDeviationDTO(
                    39.4,       // distanceFromRoute
                    false,      // isOffRoute
                    2123.3,     // distance
                    12434.2     // duration
            );

            PreviousStateDTO previousEta = new PreviousStateDTO(
                    PREVIOUS_DURATION,   // durationRemaining
                    28468753.2,          // distanceRemaining
                    INITIAL_TIME         // timestamp
            );

            long FINAL_TIME = INITIAL_TIME + 60000L;

            when(travelRepository.findById(travel.getId()))
                    .thenReturn(Optional.of(travel));

            when(routeCalculationService.isRouteDeviation(
                    currentLat, currentLng, travel.getPolylineRoute()))
                    .thenReturn(deviation);

            when(redisTrackingService.getPreviousEta(travel.getId().toString()))
                    .thenReturn(previousEta);

            when(clock.millis()).thenReturn(FINAL_TIME);

            travelTrackingService.processNewLocation(travel.getId(), currentLat, currentLng);

//            verify(redisTrackingService).storeLiveLocation(
//                    eq(travel.getId().toString()),
//                    eq(currentLat.toString()),
//                    eq(currentLng.toString()),
//                    durationCaptor.capture()
//            );

            verify(redisTrackingService).storeTravelMetadata(
                    eq(travel.getId().toString()),
                    polylineCaptor.capture(),
                    eq(currentDistance.toString()),
                    eq(travel.getTravelStatus().toString())
            );

            assertEquals(String.valueOf(EXPECTED_DURATION), durationCaptor.getValue());
            assertEquals(currentPolyline, polylineCaptor.getValue());

            verify(mapboxAPIService, never()).recalculateETA(any(), any(), any(), any());
        }

        @DisplayName("Deve lançar exceção quando a viagem não for encontrada")
        @Test
        void throwExceptionWhenTripNotFound() {
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            Double currentLng = -21.832;
            Double currentLat = -11.321;

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.empty());

            TripNotFound expectedErrorMsg = assertThrows(TripNotFound.class, () -> {
                travelTrackingService.processNewLocation(travel.getId(), currentLat, currentLng);
            });

            assertEquals("Trip not found", expectedErrorMsg.getMessage());

            verify(routeCalculationService, never()).isRouteDeviation(any(), any(), anyString());
            verify(mapboxAPIService, never()).recalculateETA(any(), any(), any(), any());
            verify(redisTrackingService, never()).getPreviousEta(anyString());
        }

        @DisplayName("Deve lançar exceção quando o TravelStatus não for TRAVELLING")
        @Test
        void throwExceptionWhenTravelStatusIsNotTravelling() {
            travel.setTravelStatus(TravelStatus.PENDING);

            Double currentLng = -21.832;
            Double currentLat = -11.321;

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            TravelException expectedErrorMsg = assertThrows(TravelException.class, () -> {
                travelTrackingService.processNewLocation(travel.getId(), currentLat, currentLng);
            });

            assertEquals("A viagem não está em andamento", expectedErrorMsg.getMessage());

            verify(routeCalculationService, never()).isRouteDeviation(any(), any(), anyString());
            verify(mapboxAPIService, never()).recalculateETA(any(), any(), any(), any());
            verify(redisTrackingService, never()).getPreviousEta(anyString());
        }

        @DisplayName("Deve lançar exceção quando o service de recalcular o ETA falhar")
        @Test
        void shouldThrowRecalculateEtaExceptionWhenRecalculationAPIFails() {
            travel.setTravelStatus(TravelStatus.TRAVELLING);

            when(routeCalculationService.isRouteDeviation(any(), any(), anyString())).thenReturn(routeDeviationDTO);

            when(travelRepository.findById(travel.getId())).thenReturn(Optional.of(travel));

            Double currentLng = -21.832;
            Double currentLat = -11.321;

            assertThrows(RecalculateEtaException.class, () -> {
                travelTrackingService.processNewLocation(travel.getId(), currentLat, currentLng);
            });

//            verify(redisTrackingService, never()).storeLiveLocation(anyString(), any(), any(), anyString());
            verify(redisTrackingService, never()).storeTravelMetadata(anyString(), any(), any(), anyString());
        }
    }

    private Travel createTravelEntity(UUID id, TravelStatus travelStatus, Driver driver, Set<StudentTravel> studentTravels, Instant startHourTravel, Instant endHourTravel, String polylineRoute, Double duration, Double distance, Double originLatitude, Double originLongitude, Double finalLatitude, Double finalLongitude) {
        return new Travel(id, travelStatus, driver, studentTravels, startHourTravel, endHourTravel, polylineRoute, duration, distance, originLatitude, originLongitude, finalLatitude, finalLongitude);
    }

}