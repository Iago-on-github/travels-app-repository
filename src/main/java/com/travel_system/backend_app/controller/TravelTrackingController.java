package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.mapboxApi.LiveLocationDTO;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.service.TravelTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/travel/tracking")
public class TravelTrackingController {

    private final TravelTrackingService travelTrackingService;

    public TravelTrackingController(TravelTrackingService travelTrackingService) {
        this.travelTrackingService = travelTrackingService;
    }

    @GetMapping("/{travelId}/location")
    public ResponseEntity<Void> processNewLocation(@PathVariable UUID travelId, @RequestParam Double lat, @RequestParam Double lng) {
        travelTrackingService.processNewLocation(travelId, lat, lng);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirmEmbark/{studentId}/{travelId}")
    public ResponseEntity<Void> confirmStudentEmbark(@PathVariable UUID studentId, @PathVariable UUID travelId) {
        travelTrackingService.confirmEmbarkOnTravel(studentId, travelId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/fastview/{travelId}")
    public ResponseEntity<LiveLocationDTO> getDriverPosition(@PathVariable UUID travelId) {
        return ResponseEntity.ok().body(travelTrackingService.getDriverPosition(travelId));
    }
}
