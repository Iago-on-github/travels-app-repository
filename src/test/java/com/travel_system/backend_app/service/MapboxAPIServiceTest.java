package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.travel_system.backend_app.customExceptions.NoSuchCoordinates;
import com.travel_system.backend_app.model.dtos.mapboxApi.*;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapboxAPIServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======
    - Os testes de cada method devem ser isolados por classes anotadas com @nested

    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.

    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer

    - Em cenários de Success, os métodos devem conter "With Success" ou "Successfully" em seus respectivos nomes.

    - Em cenários de Error, os métodos devem conter "ShouldThrowExceptionWhen..." em seus respectivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP/ACT (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private MapboxAPIService mapboxAPIService;
    @Mock
    private WebClient webClient;
    @Mock
    private TravelRepository travelRepository;
    @Mock
    private PolylineService polylineService;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;

    private Double originLong = 123234.2;
    private Double originLat = 46734.0;
    private Double destLong = 1466542.9;
    private Double destLat = 0399384.6;

    private RouteDetailsDTO routeDetailsDTO = new RouteDetailsDTO(300.0, 2000.3, "asjdoihsokoi443sa");

    private LegsDTO legsDTO = new LegsDTO(
            List.of("teste1", "teste2"),
            List.of(new AdminDTO("Admin2", "Admin3"), new AdminDTO("Admin4", "Admin5")),
            30.0,
            250.3,
            List.of("1223", "3221"),
            4000.9,
            "summary return"
    );

    private RoutesDTO routesDTO = new RoutesDTO(
            "weight_name teste",
            30.1,
            200.55,
            3000.2,
            List.of(legsDTO),
            "geometry teste"
    );

    private WaypointsDTO waypointsDTO = new WaypointsDTO(
            3230.2,
            "waypoint name",
            List.of(323.2, 120.2, 233.1)
    );

    private MapboxApiResponse apiResponse = new MapboxApiResponse(
            List.of(routesDTO),
            List.of(waypointsDTO),
            "Code teste",
            "uuid teste"
    );

    @Nested
    class calculateRoute {
        @DisplayName("Deve realizar a chamada da API e retornar os dados brutos com sucesso")
        @Test
        void shouldCalculateRouteWithSuccess() {
            String waypoints = originLong + "," + originLat + ";" + destLong + "," + destLat;

            // configura o retorno do webClient.get()
            doReturn(requestHeadersUriSpec)
                    .when(webClient).get();

            // configura o .uri para aceitar qualquer uri após a chamada do .get()
            doReturn(requestHeadersSpec)
                    .when(requestHeadersUriSpec).uri(any(Function.class));

            // configura o retrive()
            doReturn(responseSpec)
                    .when(requestHeadersSpec).retrieve();

            // configurar o bodyToMono para simular a resposta final
            doReturn(Mono.just(apiResponse))
                    .when(responseSpec).bodyToMono(MapboxApiResponse.class);

            RouteDetailsDTO result = mapboxAPIService.calculateRoute(originLong, originLat, destLong, destLat);

            final Double EXPECTED_DURATION = roundingValues(apiResponse.routes().getFirst().duration());
            final Double EXPECTED_DISTANCE = roundingValues(apiResponse.routes().getFirst().distance());

            assertNotNull(result);

            assertEquals(result.duration(), EXPECTED_DURATION);
            assertEquals(result.distance(), EXPECTED_DISTANCE);
            assertEquals(result.geometry(), apiResponse.routes().getFirst().geometry());
        }

        @DisplayName("Deve lançar exceção quando ")
        @Test
        void throwNewExceptionWhenNoSuchCoordinates() {
            Double originLong = null;
            Double originLat = 46734.0;
            Double destLong = 1466542.9;
            Double destLat = 0399384.6;

            NoSuchCoordinates exception = assertThrows(NoSuchCoordinates.class, () -> {
                mapboxAPIService.calculateRoute(originLong, originLat, destLong, destLat);
            });

            verify(webClient, never()).get();
        }
        /*
         metodo auxiliar para realizar o arredondamento dos valores
        */
        private Double roundingValues(Double value) {
            if (value == null) {
                return 0.0;
            }
            return (double) Math.round(value);
        }
    }

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
                    .FormattedPolyline(anyString());

            RouteDeviationDTO result = mapboxAPIService.isRouteDeviation(currentLat, currentLng, polyline);

            assertFalse(result.isOffRoute());
        }

        @DisplayName("Deve verificar se a localização do motorista está distante à rota padrão (isRouteOff = true)")
        @Test
        void shouldCheckIfRouteDeviationWhenOffRoute() {

        }


    }
}