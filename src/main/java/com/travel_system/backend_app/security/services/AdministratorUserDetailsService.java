package com.travel_system.backend_app.security.services;

import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.repository.AdministratorRepository;
import com.travel_system.backend_app.security.AdministratorUserDetails;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class AdministratorUserDetailsService implements UserDetailsService {
    private AdministratorRepository administratorRepository;

    public AdministratorUserDetailsService(AdministratorRepository administratorRepository) {
        this.administratorRepository = administratorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        Administrator administrator = administratorRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Administrador não encontrado"));

        return new AdministratorUserDetails(administrator);
    }
}
