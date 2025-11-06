package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.DriverRequestDTO;
import com.travel_system.backend_app.model.dtos.DriverResponseDTO;
import com.travel_system.backend_app.model.enums.Role;
import com.travel_system.backend_app.repository.UserModelRepository;
import org.hibernate.query.Page;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    /*
    ====== ORGANIZAÇÃO DOS TESTES ======
    - Os testes de cada method devem ser isolados por classes anotadas com @nested

    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.

    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer

    - Em cenários de Success, os métodos devem conter "With Success" em seus respectivos nomes.

    - Em cenários de Error, os métodos devem conter "ShouldThrowExceptionWhen..." em seus respectivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP/ACT (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private DriverService driverService;

    @Mock
    private UserModelRepository repository;
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

    @Nested
    class getAllDrivers {
        @DisplayName("Deve retornar todos os motoristas com sucesso")
        @Test
        void getAllDriversWithSuccess() {
            // MOCK CONFIGURADO PARA RETORNAR A LISTA JA MISTA, O "ACT" DOS TESTES ACONTECE AQUI
            List<UserModel> mixedList = new ArrayList<>();
            mixedList.add(driver1);
            mixedList.add(student);
            mixedList.add(driver2);

            doReturn(mixedList).when(repository).findAll();

            List<DriverResponseDTO> result = driverService.getAllDrivers();

            DriverResponseDTO expectedDto1 = createdExpectedDTO(driver1);
            DriverResponseDTO expectedDto2 = createdExpectedDTO(driver2);

            assertEquals(2, result.size(), "The filter should maintain only two drivers");

            assertEquals(expectedDto1.name(), result.get(0).name(), "O nome do primeiro driver deve ser o esperado.");
            assertEquals(expectedDto2.email(), result.get(1).email(), "O email do segundo driver deve ser o esperado.");

            verify(repository).findAll();
        }

        @DisplayName("Deve checar se o findAll esta executando, mas não está retornando " +
                "registros de Drivers. Não deve lançar exceção")
        @Test
        void shouldCheckIfFindAllExecutedButDidNotBringRecords() {
            // ACT PARA UMA LISTA SEM REGISTRO DE MOTORISTAS
            List<UserModel> mixedListWithoutDrivers = new ArrayList<>();
            mixedListWithoutDrivers.add(null);
            mixedListWithoutDrivers.add(null);
            mixedListWithoutDrivers.add(null);

            doReturn(mixedListWithoutDrivers).when(repository).findAll();

            List<DriverResponseDTO> result = driverService.getAllDrivers();

            assertEquals(0, result.size(), "The filter not should be return any driver ");

            verify(repository).findAll();
        }

        @DisplayName("Deve lançar execeção se o repositório não retornar absolutamente nada")
        @Test
        void shouldThrowExceptionWhenFindAllIsEmpty() {
            // ACT CONFIGURADO PARA RETORNAR LISTA VAZIA
            doReturn(Collections.emptyList()).when(repository).findAll();

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                driverService.getAllDrivers();
            });

            assertEquals("motoristas não encontrados", thrown.getMessage(),
                    "A execução deve conter a mensagem esperada");

            verify(repository).findAll();
        }
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