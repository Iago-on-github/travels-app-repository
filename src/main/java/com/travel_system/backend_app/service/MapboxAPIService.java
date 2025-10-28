package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.travel_system.backend_app.interfaces.MapboxAPICalling;
import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import com.travel_system.backend_app.model.dtos.mapboxApi.RoutesDTO;
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

    // verifica se a rota foi desviada do padrão
    public RouteDeviationDTO isRouteDeviation(double currentLat, double currentLong, String polylineRoute) {
        final int precision = 5;

        double minDistance = Double.MAX_VALUE;
        double projLng = 0;
        double projLat = 0;

        List<Point> decodePolyline = PolylineUtils.decode(polylineRoute, precision);
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
