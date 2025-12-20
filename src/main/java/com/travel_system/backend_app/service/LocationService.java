package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.NoSuchCoordinates;
import com.travel_system.backend_app.model.dtos.mapboxApi.LiveCoordinates;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    public LiveCoordinates processLocation(LiveCoordinates liveCoordinates) {
        if (liveCoordinates.latitude() == null || liveCoordinates.longitude() == null) {
            throw new NoSuchCoordinates("Coordenadas lat/lng corrompidas ou não enviadas.");
        }

        if (liveCoordinates.latitude() < -90 || liveCoordinates.latitude() > 90 ||
                liveCoordinates.longitude() < -180 || liveCoordinates.longitude() > 180) {
            throw new NoSuchCoordinates("Valores de latitude ou longitude fora do intervalo válido.");
        }

        Point point = new Point(liveCoordinates.latitude(), liveCoordinates.longitude());
        return null;
    }
}
