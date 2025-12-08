package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.mapboxApi.RouteDeviationDTO;
import com.travel_system.backend_app.service.RouteCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/routeCalculation")
public class RouteCalculationController {
    private final RouteCalculationService routeCalculationService;

    public RouteCalculationController(RouteCalculationService routeCalculationService) {
        this.routeCalculationService = routeCalculationService;
    }

    @GetMapping("/deviation")
    public ResponseEntity<RouteDeviationDTO> isRouteDeviation(@RequestParam Map<String, String> params) {
        Double currentLat = getDoubleParams(params, "currentLat");
        Double currentLong = getDoubleParams(params, "currentLong");
        String polylineRoute = params.get("polylineRoute");

        RouteDeviationDTO deviationDTO = routeCalculationService.isRouteDeviation(currentLat, currentLong, polylineRoute);
        return ResponseEntity.ok().body(deviationDTO);
    }

    private Double getDoubleParams(Map<String, String> params, String key) {
        String value = params.get(key);
        return value != null ? Double.valueOf(value) : null;
    }
}
