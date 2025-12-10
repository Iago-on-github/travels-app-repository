package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import com.travel_system.backend_app.interfaces.MapboxAPICalling;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RoutesDTO;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;


@Service
public class MapboxAPIService implements MapboxAPICalling {
    @Value("${mapbox.access.token}")
    private String accessToken;

    private final WebClient webClient;
    private final TravelRepository travelRepository;

    @Autowired
    public MapboxAPIService(WebClient webClient, TravelRepository travelRepository) {
        this.webClient = webClient;
        this.travelRepository = travelRepository;
    }

    // chamada bruta da api
    @Override
    public RouteDetailsDTO calculateRoute(Double originLong, Double originLat, Double destLong, Double destLat) {
        String waypoints = originLong + "," + originLat + ";" + destLong + "," + destLat;

        if (originLong == null || originLat == null || destLong == null || destLat == null) {
            throw new NoSuchCoordinates("Coordenadas não encontradas, "
                    + "originLong: " + originLong
                    + "originLong: " + originLat
                    + "destLong: " + destLong
                    + "destLat:" + destLat);
        }

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

    // retorna distância/tempo restante com base na localização atual
    public RouteDetailsDTO recalculateETA(Double currentLng, Double currentLat, Double finalLong, Double finalLat) {
        RouteDetailsDTO routeDetails = calculateRoute(currentLng, currentLat, finalLong, finalLat);

        if (routeDetails == null) throw new NoSuchCoordinates("Sem dados de rota");

        return routeDetails;
    }

    // salva os dados de distance, duration e polyline na entidade Travel
    @Transactional
    public void getRouteDetailsDTO(Double originLong, Double originLat, Double destLong, Double destLat) {
        RouteDetailsDTO staticRouteDetails = calculateRoute(originLong, originLat, destLong, destLat);

        if (staticRouteDetails == null) throw new NoSuchCoordinates("Dados de rota estão nulos");

        travelRepository.save(travelMapper(staticRouteDetails));
    }

    // padroniza a decodificação do polyline
    private Travel travelMapper(RouteDetailsDTO routeDetailsDTO) {
        Travel travelEntity = new Travel();

        travelEntity.setDistance(routeDetailsDTO.distance());
        travelEntity.setDuration(routeDetailsDTO.duration());
        travelEntity.setPolylineRoute(routeDetailsDTO.geometry());

        return travelEntity;
    }

    private RouteDetailsDTO RouteDetailsMapper(MapboxApiResponse mapboxApiResponse) {
        if (mapboxApiResponse.routes().isEmpty()) {
            throw new NoSuchCoordinates("Routes is empty.");
        }

        RoutesDTO routesDto = mapboxApiResponse.routes().getFirst();

        return new RouteDetailsDTO(
                (double) Math.round(routesDto.duration()),
                (double) Math.round(routesDto.distance()),
                routesDto.geometry()
        );
    }
}
