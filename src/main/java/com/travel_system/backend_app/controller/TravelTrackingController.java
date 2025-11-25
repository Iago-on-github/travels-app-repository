package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.service.TravelTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/travel/tracking")
public class TravelTrackingController {

    private TravelTrackingService travelTrackingService;

    @Autowired
    public TravelTrackingController(TravelTrackingService travelTrackingService) {
        this.travelTrackingService = travelTrackingService;
    }

    @GetMapping("/{travelId}")
    public ResponseEntity<Void> processNewLocation(@PathVariable UUID travelId, @RequestParam Double currentLat, @RequestParam Double currentLng) {
        travelTrackingService.processNewLocation(travelId, currentLat, currentLng);
        return ResponseEntity.noContent().build();
    }
}
