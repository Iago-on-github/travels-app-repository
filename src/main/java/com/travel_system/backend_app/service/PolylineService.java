package com.travel_system.backend_app.service;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.utils.PolylineUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolylineService {

    public List<Point> FormattedPolyline(String polylineRoute) {
        int precision = 5;
        return PolylineUtils.decode(polylineRoute, precision);
    }
}
