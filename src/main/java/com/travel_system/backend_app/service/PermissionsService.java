package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.PermissionNotFoundException;
import com.travel_system.backend_app.model.Permissions;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.repository.PermissionsRepository;
import com.travel_system.backend_app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionsService {

    private final UserRepository userRepository;
    private final PermissionsRepository permissionsRepository;

    public PermissionsService(UserRepository userRepository, PermissionsRepository permissionsRepository) {
        this.userRepository = userRepository;
        this.permissionsRepository = permissionsRepository;
    }

    public void assignPermissions(UUID id, String permission) {
        UserModel expectedUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado " + id));
        Permissions perm = permissionsRepository.findByDescription(permission)
                .orElseThrow(() -> new PermissionNotFoundException("Permissão não encontrada"));

        expectedUser.getPermissions().add(perm);
        userRepository.save(expectedUser);
    }

}
