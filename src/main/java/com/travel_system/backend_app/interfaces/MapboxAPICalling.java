package com.travel_system.backend_app.interfaces;

import com.travel_system.backend_app.model.dtos.mapboxApi.MapboxApiResponse;
import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDetailsDTO;

public interface MapboxAPICalling {
    // define os contratos de chamadas da api

    RouteDetailsDTO calculateRoute(double originLat, double originLong, double destLat, double destLong);
}
