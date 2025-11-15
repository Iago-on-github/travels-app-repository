package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.DriverRequestDTO;
import com.travel_system.backend_app.model.dtos.DriverResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.repository.UserModelRepository;
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
public class DriverService {
    private UserModelRepository repository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public DriverService(UserModelRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<DriverResponseDTO> getAllDrivers() {
        List<UserModel> allDrivers = repository.findAll();
        if (allDrivers.isEmpty()) {
            return Collections.emptyList();
        }
        return allDrivers.stream().filter(driver -> driver instanceof Driver)
                .map(driver -> driverConverted((Driver) driver))
                .toList();
    }

    public List<DriverResponseDTO> getAllActiveDrivers() {
        return getDriversByStatus(GeneralStatus.ACTIVE);
    }

    public List<DriverResponseDTO> getAllInactiveDrivers() {
        return getDriversByStatus(GeneralStatus.INACTIVE);
    }

    @Transactional
    public DriverResponseDTO createDriver(DriverRequestDTO driverRequestDTO) {
        Driver newDriver = driverMapper(driverRequestDTO);

        verifyFieldsIsNull(driverRequestDTO);

        Optional<UserModel> email = repository.findByEmail(newDriver.getEmail());
        Optional<UserModel> telephone = repository.findByTelephone(newDriver.getTelephone());

        if (email.isPresent()) throw new RuntimeException("Email já existe");
        if (telephone.isPresent()) throw new RuntimeException("Telefone já existe");

        // cryptography the password
        String rawPassword = newDriver.getPassword();
        newDriver.setPassword(passwordEncoder.encode(rawPassword));

        Driver savedDriver = repository.save(newDriver);
        return driverConverted(savedDriver);
    }

    @Transactional
    public DriverResponseDTO updateLoggedDriver(String authenticatedEmail, DriverRequestDTO driverRequestDTO) {
        Driver driverLogged = (Driver) repository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("Motorista não encontrado, " + authenticatedEmail));

        if (driverLogged.getStatus().equals(GeneralStatus.INACTIVE)) {
            throw new RuntimeException("Não é possível modificar dados de uma conta inativa");
        }

        Optional<UserModel> existingUser = repository.findByEmailOrTelephoneAndIdNot(
                driverRequestDTO.email(),
                driverRequestDTO.telephone(),
                driverLogged.getId()
        );
        if (existingUser.isPresent()) throw new RuntimeException("Email ou telefone já em uso por outro usuário.");

        driverLogged.setEmail(driverRequestDTO.email());
        driverLogged.setPassword(driverRequestDTO.password());
        driverLogged.setName(driverRequestDTO.name());
        driverLogged.setLastName(driverRequestDTO.lastName());
        driverLogged.setTelephone(driverRequestDTO.telephone());
        driverLogged.setProfilePicture(driverRequestDTO.profilePicture());
        driverLogged.setAreaOfActivity(driverRequestDTO.areaOfActivity());

        Driver savedDriver = repository.save(driverLogged);
        return driverConverted(savedDriver);
    }

    public DriverResponseDTO getLoggedInDriverProfile(String email, String telephone) {
        Driver getDriverLoggedProfile = (Driver) repository.findByEmailOrTelephone(email, telephone)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado. Email, telephone: " + email + ", " + telephone));
        return driverConverted(getDriverLoggedProfile);
    }

    @Transactional
    public void disableDriver(UUID id) {
        Optional<UserModel> driver = repository.findById(id);
        UserModel userModel = driver.orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado, " + id));

        if (userModel instanceof Driver driverRequest) {
            if (driverRequest.getStatus().equals(GeneralStatus.INACTIVE)) {
                throw new IllegalStateException("Driver já desativado, " + id);
            }
            driverRequest.setStatus(GeneralStatus.INACTIVE);
            repository.save(driverRequest);
        } else {
            throw new IllegalArgumentException("Usuário não é um motorista, " + id);
        }
    }



    // METODOS AUXILIARES
    // METODOS AUXILIARES
    // METODOS AUXILIARES

    private List<DriverResponseDTO> getDriversByStatus(GeneralStatus status) {
        List<UserModel> drivers = repository.findAllByStatus(status);
        if (drivers.isEmpty()) {
            return Collections.emptyList();
        }
        return drivers.stream().filter(driver -> driver instanceof Driver)
                .map(driver -> driverConverted((Driver) driver))
                .toList();
    }

    private Driver driverMapper(DriverRequestDTO requestDTO) {
        Driver newDriver = new Driver();

        newDriver.setEmail(requestDTO.email());
        newDriver.setPassword(requestDTO.password());
        newDriver.setName(requestDTO.name());
        newDriver.setLastName(requestDTO.lastName());
        newDriver.setTelephone(requestDTO.telephone());
        newDriver.setProfilePicture(requestDTO.profilePicture());
        newDriver.setAreaOfActivity(requestDTO.areaOfActivity());

        return newDriver;
    }

    private void verifyFieldsIsNull(DriverRequestDTO dto) {
        if (dto.email() == null || dto.password() == null ||
                dto.name() == null || dto.telephone() == null || dto.areaOfActivity() == null) {
            throw new RuntimeException("Você deve preencher todos os campos requeridos");
        }
    }

    private DriverResponseDTO driverConverted(Driver driver) {
        return new DriverResponseDTO(
                driver.getId(),
                driver.getName(),
                driver.getLastName(),
                driver.getEmail(),
                driver.getTelephone(),
                driver.getCreatedAt(),
                driver.getAreaOfActivity(),
                driver.getTotalTrips()
        );
    }
}
