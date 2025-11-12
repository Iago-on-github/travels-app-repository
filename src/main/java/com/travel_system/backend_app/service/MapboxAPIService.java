package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.travel_system.backend_app.customExceptions.NoSuchCoordinates;
import com.travel_system.backend_app.interfaces.MapboxAPICalling;
import com.travel_system.backend_app.model.Travel;
import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RoutesDTO;
import com.travel_system.backend_app.repository.TravelRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class MapboxAPIService implements MapboxAPICalling {
    @Value("${mapbox.access.token}")
    private String accessToken;

    private WebClient webClient;
    private TravelRepository travelRepository;
    private PolylineService polylineService;

    @Autowired
    public MapboxAPIService(PolylineService polylineService, TravelRepository travelRepository, WebClient webClient) {
        this.polylineService = polylineService;
        this.travelRepository = travelRepository;
        this.webClient = webClient;
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

    // verifica se a rota foi desviada do padrão
    public RouteDeviationDTO isRouteDeviation(Double currentLat, Double currentLong, String polylineRoute) {
        double minDistance = Double.MAX_VALUE;
        double projLng = 0;
        double projLat = 0;

        if (currentLat == null || currentLong == null || polylineRoute == null) {
            throw new NoSuchCoordinates("Coordenadas atuais não encontradas, "
                    + "currentLat: " + currentLat
                    + "currentLong: " + currentLong
                    + "polylineRoute: " + polylineRoute);
        }

        List<Point> decodePolyline = polylineService.formattedPolyline(polylineRoute);
        Point driverCurrentLoc = Point.fromLngLat(currentLong, currentLat);

        if (decodePolyline.size() < 2) {
            return new RouteDeviationDTO(0, false, currentLat, currentLong);
        }

        for (int i = 0; i < decodePolyline.size() - 1; i++) {
            Point P_A = decodePolyline.get(i);
            Point P_B = decodePolyline.get(i + 1);

            double[] routeDirection = {P_B.longitude() - P_A.longitude(), P_B.latitude() - P_A.latitude()};
            double[] routePoint = {driverCurrentLoc.longitude() - P_A.longitude(), driverCurrentLoc.latitude() - P_A.latitude()};

            double dotWV = routePoint[0] * routeDirection[0] + routePoint[1] * routeDirection[1];
            double dotVV = routeDirection[0] * routeDirection[0] + routeDirection[1] * routeDirection[1];

            double t = dotWV / dotVV;
            if (t < 0) t = 0;
            if (t > 1) t = 1;

            double projLngCandidate = P_A.longitude() + t * routeDirection[0];
            double projLatCandidate = P_A.latitude() + t * routeDirection[1];

            double distance = Math.sqrt(Math.pow(driverCurrentLoc.longitude() - projLngCandidate, 2)
                    + Math.pow(driverCurrentLoc.latitude() - projLatCandidate, 2));

            if (distance < minDistance) {
                minDistance = distance;
                projLng = projLngCandidate;
                projLat = projLatCandidate;
            }
        }

        double minDistanceMeters = minDistance * 111320;
        boolean isOffRoute = minDistanceMeters > 50; // tolerância de 50m

        double finalDistance = !decodePolyline.isEmpty() ? minDistanceMeters : 0;

        return new RouteDeviationDTO(
                finalDistance,
                isOffRoute,
                projLat,
                projLng
        );
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
