package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.model.dtos.request.AdministratorRequestDTO;
import com.travel_system.backend_app.model.dtos.response.AdministratorResponseDTO;
import com.travel_system.backend_app.repository.AdministratorRepository;
import com.travel_system.backend_app.service.AdministratorService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admins")
public class AdministratorController {

    private AdministratorService administratorService;

    @Autowired
    public AdministratorController(AdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    @GetMapping
    public ResponseEntity<List<AdministratorResponseDTO>> getAllAdmins() {
        return ResponseEntity.ok().body(administratorService.getAllAdministrators());
    }

    @GetMapping("/active")
    public ResponseEntity<List<AdministratorResponseDTO>> getAllActiveAdmins() {
        return ResponseEntity.ok().body(administratorService.getAllActiveAdministrators());
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<AdministratorResponseDTO>> getAllInactiveAdmins() {
        return ResponseEntity.ok().body(administratorService.getAllInactiveAdministrators());
    }

    @GetMapping("/logged/{email}")
    public ResponseEntity<AdministratorResponseDTO> getLoggedAdministratorInProfile(@PathVariable String email) {
        return ResponseEntity.ok().body(administratorService.getLoggedAdministratorInProfile(email));
    }

    @PostMapping("/new")
    public ResponseEntity<AdministratorResponseDTO> createAdministrator(@RequestBody AdministratorRequestDTO admRequestDTO, UriComponentsBuilder componentsBuilder) {
        AdministratorResponseDTO newAdm = administratorService.createAdministrator(admRequestDTO);

        URI uri = componentsBuilder.path("/{id}").buildAndExpand(newAdm.id()).toUri();

        return ResponseEntity.created(uri).body(newAdm);
    }

    @PutMapping("/disable/{id}")
    public ResponseEntity<Void> disableAdministrator(@PathVariable UUID id) {
        administratorService.disableAdministrator(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/enable/{id}")
    public ResponseEntity<Void> enableAdministrator(@PathVariable UUID id) {
        administratorService.enableAdministrator(id);
        return ResponseEntity.noContent().build();
    }

}
