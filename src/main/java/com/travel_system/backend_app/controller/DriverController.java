package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.DriverRequestDTO;
import com.travel_system.backend_app.model.dtos.DriverResponseDTO;
import com.travel_system.backend_app.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/drivers/api/v1")
public class DriverController {
    private DriverService driverService;

    @Autowired
    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<List<DriverResponseDTO>> getAllDrivers() {
        return ResponseEntity.ok().body(driverService.getAllDrivers());
    }

    @GetMapping("/active")
    public ResponseEntity<List<DriverResponseDTO>> getAllActiveDrivers() {
        return ResponseEntity.ok().body(driverService.getAllActiveDrivers());
    }

    @GetMapping("/Inactive")
    public ResponseEntity<List<DriverResponseDTO>> getAllInactiveDrivers() {
        return ResponseEntity.ok().body(driverService.getAllInactiveDrivers());
    }

    @GetMapping("/{email}/{telephone}")
    public ResponseEntity<DriverResponseDTO> getLoggedInDriverProfile(@PathVariable String email, @PathVariable String telephone) {
        DriverResponseDTO loggedDriver = driverService.getLoggedInDriverProfile(email, telephone);
        return ResponseEntity.ok().body(loggedDriver);
    }

    @PostMapping()
    public ResponseEntity<DriverResponseDTO> createDriver(@RequestBody DriverRequestDTO driverRequestDTO, UriComponentsBuilder componentsBuilder) {
        DriverResponseDTO newDriver = driverService.createDriver(driverRequestDTO);
        URI uri = componentsBuilder.path("/{id}").buildAndExpand(newDriver.id()).toUri();
        return ResponseEntity.created(uri).body(newDriver);
    }

    @PutMapping("/{authenticatedEmail}")
    public ResponseEntity<DriverResponseDTO> updateLoggedDriver(@PathVariable String authenticatedEmail, @RequestBody DriverRequestDTO driverRequestDTO) {
        DriverResponseDTO loggedDriver = driverService.updateLoggedDriver(authenticatedEmail, driverRequestDTO);
        return ResponseEntity.ok().body(loggedDriver);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> disableDriver(@PathVariable UUID id) {
        driverService.disableDriver(id);
        return ResponseEntity.noContent().build();
    }
}
