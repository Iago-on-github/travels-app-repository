package com.travel_system.backend_app.service;

import com.travel_system.backend_app.interfaces.MapboxAPICalling;
import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import org.apache.tomcat.websocket.server.UriTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

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
    public MapboxApiResponse calculateRoute(double originLong, double originLat, double destLong, double destLat) {
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
                .block();
    }
}
