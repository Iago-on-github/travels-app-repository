package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.DuplicateResourceException;
import com.travel_system.backend_app.exceptions.EmptyMandatoryFieldsFound;
import com.travel_system.backend_app.exceptions.InactiveAccountModificationException;
import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.model.dtos.request.AdministratorRequestDTO;
import com.travel_system.backend_app.model.dtos.response.AdministratorResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.repository.AdministratorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdministratorService {
    private AdministratorRepository administratorRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AdministratorService(AdministratorRepository administratorRepository, PasswordEncoder passwordEncoder) {
        this.administratorRepository = administratorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdministratorResponseDTO> getAllAdministrators() {
        List<Administrator> allAdmins = administratorRepository.findAll();

        if (allAdmins.isEmpty()) return Collections.emptyList();

        return allAdmins.stream().map(this::admConverted).toList();
    }

    public List<AdministratorResponseDTO> getAllActiveAdministrators() {
        return findAdmsByStatus(GeneralStatus.ACTIVE);
    }

    public List<AdministratorResponseDTO> getAllInactiveAdministrators() {
        return findAdmsByStatus(GeneralStatus.INACTIVE);
    }

    public AdministratorResponseDTO getLoggedAdministratorInProfile(String authenticatedAdmEmail, String authenticatedAdmTelephone) {
        Administrator expectedLoggedAdmin = administratorRepository.findByEmailOrTelephone(authenticatedAdmEmail, authenticatedAdmTelephone)
                .orElseThrow(() -> new EntityNotFoundException("Administrador não encontrado"));

        return admConverted(expectedLoggedAdmin);
    }

    @Transactional
    public AdministratorResponseDTO createAdministrator(AdministratorRequestDTO admRequestDTO) {
        Administrator adm = admMapper(admRequestDTO);

        checkFieldsIsNull(admRequestDTO);

        Optional<Administrator> existingAdministratorEmail = administratorRepository.findByEmail(adm.getEmail());
        Optional<Administrator> existingAdministratorTelephone = administratorRepository.findByTelephone(adm.getEmail());

        if (existingAdministratorEmail.isPresent()) throw new DuplicateResourceException("Email já registrado");
        if (existingAdministratorTelephone.isPresent()) throw new DuplicateResourceException("Telefone já registrado");

        String rawPassword = adm.getPassword();
        adm.setPassword(passwordEncoder.encode(rawPassword));

        Administrator savedAdm = administratorRepository.save(adm);
        return admConverted(savedAdm);
    }

    @Transactional
    public AdministratorResponseDTO updateLoggedAdministrator(String authenticatedEmail, AdministratorRequestDTO admRequestDTO) {
        Administrator loggedAdm = administratorRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new EntityNotFoundException("Administrador não encontrado, " + authenticatedEmail));

        if (loggedAdm.getStatus().equals(GeneralStatus.INACTIVE)) throw new InactiveAccountModificationException("Não é possível atualizar uma conta desativada");

        Optional<Administrator> existingAdmin = administratorRepository.findByEmailOrTelephoneAndIdNot(
                admRequestDTO.email(),
                admRequestDTO.telephone(),
                loggedAdm.getId());

        if (existingAdmin.isPresent()) throw new DuplicateResourceException("Email ou telefone já em uso por outro usuário");

        loggedAdm.setEmail(admRequestDTO.email());
        loggedAdm.setPassword(admRequestDTO.password());
        loggedAdm.setName(admRequestDTO.name());
        loggedAdm.setLastName(admRequestDTO.lastName());
        loggedAdm.setTelephone(admRequestDTO.telephone());
        loggedAdm.setProfilePicture(admRequestDTO.profilePicture());

        Administrator savedAdmin = administratorRepository.save(loggedAdm);
        return admConverted(savedAdmin);
    }

    @Transactional
    public void disableAdministrator(UUID id) {
        Administrator expectedAdm = administratorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Administrador não encontrado, " + id));

        if (expectedAdm.getStatus() == GeneralStatus.INACTIVE) throw new InactiveAccountModificationException("Desculpe, administrador já desativado");

        expectedAdm.setStatus(GeneralStatus.INACTIVE);

        administratorRepository.save(expectedAdm);
    }

    @Transactional
    public void enableAdministrator(UUID id) {
        Administrator expectedAdministrator = administratorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Administrador não encontrado"));

        if (expectedAdministrator.getStatus() == GeneralStatus.ACTIVE) throw new DuplicateResourceException("Administrador já ativo, " + id);

        expectedAdministrator.setStatus(GeneralStatus.ACTIVE);

        administratorRepository.save(expectedAdministrator);
    }

    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES

    private void checkFieldsIsNull(AdministratorRequestDTO admRequestDTO) {
       if (admRequestDTO.email() == null || admRequestDTO.password() == null ||
               admRequestDTO.name() == null || admRequestDTO.telephone() == null)  {
           throw new EmptyMandatoryFieldsFound("Você deve preencher todos os campos requeridos.");
       }
    }

    private List<AdministratorResponseDTO> findAdmsByStatus(GeneralStatus status) {
        List<Administrator> activeAdms = administratorRepository.findByStatus(status);

        if (activeAdms.isEmpty()) return Collections.emptyList();

        return activeAdms.stream().map(this::admConverted).toList();
    }

    private Administrator admMapper(AdministratorRequestDTO admRequestDto) {
        Administrator adm = new Administrator();

        adm.setEmail(admRequestDto.email());
        adm.setPassword(admRequestDto.password());
        adm.setName(admRequestDto.name());
        adm.setLastName(admRequestDto.lastName());
        adm.setTelephone(admRequestDto.telephone());
        adm.setProfilePicture(admRequestDto.profilePicture());

        return adm;
    }

    private AdministratorResponseDTO admConverted(Administrator adm) {
        return new AdministratorResponseDTO(
            adm.getEmail(),
                adm.getPassword(),
                adm.getName(),
                adm.getLastName(),
                adm.getTelephone(),
                adm.getProfilePicture(),
                adm.getStatus(),
                adm.getCreatedAt(),
                adm.getUpdatedAt()
        );
    }
}
