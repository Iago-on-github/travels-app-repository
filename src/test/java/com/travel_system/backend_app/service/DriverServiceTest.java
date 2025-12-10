package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.request.DriverRequestDTO;
import com.travel_system.backend_app.model.dtos.response.DriverResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    /*
    ====== ORGANIZAÇÃO DOS TESTES ======
    - Os testes de cada method devem ser isolados por classes anotadas com @nested

    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.

    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer

    - Em cenários de Success, os métodos devem conter "With Success" ou "Successfully" em seus respectivos nomes.

    - Em cenários de Error, os métodos devem conter "ShouldThrowExceptionWhen..." em seus respectivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP/ACT (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private DriverService driverService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DriverResponseDTO driverDto1;
    private DriverResponseDTO driverDto2;

    private Driver driver1;
    private Driver driver2;
    private Student student;

    @BeforeEach
    void setUp() {

        driver1 = createDriverForTest(UUID.randomUUID(), "Maria", "Santos", "Maria@gmail.com", "2125353452", LocalDateTime.now(),"City", 10);
        driver2 = createDriverForTest(UUID.randomUUID(), "Joao", "Silva", "Joao@gmail.com", "10390129323", LocalDateTime.now(),"Roça", 20);

        student = new Student();
    }

    // [========= MÉTODOS AUXILIARES =========]
    // [========= MÉTODOS AUXILIARES =========]
    // [========= MÉTODOS AUXILIARES =========]
    // [========= MÉTODOS AUXILIARES =========]

    private DriverResponseDTO createdExpectedDTO(Driver driver) {
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

    private Driver createDriverForTest(UUID id, String name, String lastName, String email, String telephone, LocalDateTime createdAt, String areaOfActivity, Integer totalTrips) {
        Driver newDriver = new Driver();

        newDriver.setId(id);
        newDriver.setName(name);
        newDriver.setLastName(lastName);
        newDriver.setEmail(email);
        newDriver.setTelephone(telephone);
        newDriver.setCreatedAt(createdAt);
        newDriver.setAreaOfActivity(areaOfActivity);
        newDriver.setTotalTrips(totalTrips);

        return newDriver;
    }

}