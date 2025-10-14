package com.travel_system.backend_app.model.dtos.mapboxApi;

import java.util.List;

public record MapboxApiResponse(
        List<RoutesDTO> routes,
        List<WaypointsDTO> waypoints,
        String code,
        String uuid
) {}

record WaypointsDTO(
        Double distance,
        String name,
        List<Double> location
) {}

record RoutesDTO(
        String weight_name,
        Double weight,
        Double duration,
        Double distance,
        List<LegsDTO> legs,
        String geometry
) {}

record LegsDTO(
        List<Object> via_waypoints,
        List<AdminDTO> admins,
        Double weight,
        Double duration,
        List<Object> steps,
        Double distance,
        String summary
) {}

record AdminDTO(
        String iso_3166_1_alpha3,
        String iso_3166_1
) {}

//record GeometryDTO(
//        List<List<Double>> coordinates,
//        String type
//) {}
