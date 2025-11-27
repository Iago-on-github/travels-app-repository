package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.model.dtos.AdministratorResponseDTO;
import com.travel_system.backend_app.repository.AdministratorRepository;
import com.travel_system.backend_app.security.AdministratorUserDetails;
import com.travel_system.backend_app.service.AdministratorService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.tomcat.websocket.AuthenticationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admins")
public class AdministratorController {

    private AdministratorService administratorService;
    private AdministratorRepository administratorRepository;

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

    @GetMapping("/logged")
    public ResponseEntity<AdministratorResponseDTO> getLoggedAdministratorInProfile() throws AuthenticationException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();


    }

    private AdministratorResponseDTO admConverted(Administrator adm) {
        return new AdministratorResponseDTO(
                adm.getEmail(),
                adm.getPassword(),
                adm.getName(),
                adm.getLastName(),
                adm.getRole(),
                adm.getTelephone(),
                adm.getProfilePicture(),
                adm.getStatus(),
                adm.getCreatedAt(),
                adm.getUpdatedAt()
        );
    }
}
