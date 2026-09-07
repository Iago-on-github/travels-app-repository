package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.DuplicateResourceException;
import com.travel_system.backend_app.exceptions.NotAuthorizedException;
import com.travel_system.backend_app.exceptions.PermissionNotFoundException;
import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.model.Permissions;
import com.travel_system.backend_app.model.dtos.request.PlatformAdministratorRequestDTO;
import com.travel_system.backend_app.model.dtos.response.AdministratorResponseDTO;
import com.travel_system.backend_app.repository.PlatformAdministratorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PlatformAdministratorService {

    private final PlatformAdministratorRepository platformAdministratorRepository;

    public PlatformAdministratorService(PlatformAdministratorRepository platformAdministratorRepository) {
        this.platformAdministratorRepository = platformAdministratorRepository;
    }

/*    @Transactional
    public AdministratorResponseDTO createPlatformAdministrator(PlatformAdministratorRequestDTO platformAdmRequestDTO) {
        boolean platformAdmin = currentUserService.isPlatformAdmin();

        if (!platformAdmin) {
            throw new NotAuthorizedException("Administrador sem permissão necessária para criar Administradores de Plataforma.");
        }

        checkFieldsIsNull(platformAdmRequestDTO);

        Optional<Administrator> existingAdministratorEmail = administratorRepository.findByEmail(platformAdmRequestDTO.email());
        Optional<Administrator> existingAdministratorTelephone = administratorRepository.findByTelephone(platformAdmRequestDTO.telephone());

        if (existingAdministratorEmail.isPresent()) throw new DuplicateResourceException("Email já registrado");
        if (existingAdministratorTelephone.isPresent()) throw new DuplicateResourceException("Telefone já registrado");

        final String ROLE_PLATFORM = "ROLE_PLATFORM_ADMIN";
        Permissions admPerm = permissionsRepository.findByDescription(ROLE_PLATFORM)
                .orElseThrow(() -> new PermissionNotFoundException("Permissão " + ROLE_PLATFORM + " não encontrada."));

        Administrator adm = admPlatformMapper(platformAdmRequestDTO);

        adm.setPermissions(List.of(admPerm));

        Administrator savedAdm = administratorRepository.save(adm);

        return admConverted(savedAdm);
    }*/
}
