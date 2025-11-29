package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.response.AdministratorResponseDTO;
import com.travel_system.backend_app.repository.AdministratorRepository;
import com.travel_system.backend_app.service.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admins")
public class AdministratorController {

    private AdministratorService administratorService;
    private AdministratorRepository administratorRepository;

    @Autowired
    public AdministratorController(AdministratorService administratorService, AdministratorRepository administratorRepository) {
        this.administratorService = administratorService;
        this.administratorRepository = administratorRepository;
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

//    @GetMapping("/logged")
//    public ResponseEntity<AdministratorResponseDTO> getLoggedAdministratorInProfile() throws AuthenticationException {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//    }

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
