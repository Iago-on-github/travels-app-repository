package com.travel_system.backend_app.service;

import com.travel_system.backend_app.interfaces.MapboxAPICalling;
import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RoutesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class MapboxAPIService implements MapboxAPICalling {
    @Value("${mapbox.access.token}")
    private String accessToken;

    private WebClient webClient;

    @Autowired
    public MapboxAPIService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public RouteDetailsDTO calculateRoute(double originLong, double originLat, double destLong, double destLat) {
        String waypoints = originLong + "," + originLat + ";" + destLong + "," + destLat;

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/mapbox/driving/{waypoints}")
                        .queryParam("geometries","polyline")
                        .queryParam("overview","full")
                        .queryParam("access_token", accessToken)
                        .build(waypoints))
                .retrieve()
                .bodyToMono(MapboxApiResponse.class)
                .map(this::RouteDetailsMapper)
                .block();
    }

    private RouteDetailsDTO RouteDetailsMapper(MapboxApiResponse mapboxApiResponse) {
        if (mapboxApiResponse.routes().isEmpty()) {
            throw new RuntimeException("Routes is empty.");
        }

        RoutesDTO routesDto = mapboxApiResponse.routes().getFirst();

        return new RouteDetailsDTO(
                Math.round(routesDto.distance()),
                Math.round(routesDto.duration()),
                routesDto.geometry()
        );
    }
}
