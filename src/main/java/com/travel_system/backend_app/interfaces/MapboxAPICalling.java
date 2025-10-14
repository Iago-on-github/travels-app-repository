package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;

public interface MapboxAPICalling {
    // define os contratos de chamadas da api

    MapboxApiResponse calculateRoute(double originLat, double originLong, double destLat, double destLong);
}
