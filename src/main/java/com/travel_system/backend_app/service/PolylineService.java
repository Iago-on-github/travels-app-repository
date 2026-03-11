package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolylineService {

    public List<Point> formattedPolylineDecoded(String polylineRoute) {
        int precision = 5;

        if (polylineRoute.isEmpty()) {
            throw new NoSuchCoordinates("Polyline está vazio, " + polylineRoute);
        }

        return PolylineUtils.decode(polylineRoute, precision);
    }

    public String formattedPolylineEncoded(List<Point> polylineRoute) {
        int precision = 5;

        if (polylineRoute.isEmpty()) {
            throw new NoSuchCoordinates("Polyline está vazio, " + polylineRoute);
        }

        return PolylineUtils.encode(polylineRoute, precision);
    }
}
