package com.travel_system.backend_app.model.dtos.mapboxApi;

import java.util.List;

public record MapboxApiResponse(
        List<RoutesDTO> routes,
        List<WaypointsDTO> waypoints,
        String code,
        String uuid
) {}
