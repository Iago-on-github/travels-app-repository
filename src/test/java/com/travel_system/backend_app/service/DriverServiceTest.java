package com.travel_system.backend_app.service;

import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.DriverRequestDTO;
import com.travel_system.backend_app.model.dtos.DriverResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.repository.UserModelRepository;
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

        @DisplayName("Deve retornar uma lista vazia caso não haja registros no banco")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            // ACT CONFIGURADO PARA RETORNAR LISTA VAZIA
            doReturn(Collections.emptyList()).when(repository).findAll();

            List<DriverResponseDTO> result = driverService.getAllDrivers();

            assertEquals(0, result.size());

            verify(repository).findAll();
        }
    }

    @Nested
    class getAllActiveDrivers {

        @DisplayName("Deve retornar todos os motoristas ATIVOS com sucesso")
        @Test
        void getAllActiveDriversSuccessfully() {
            List<UserModel> activeDriversList = new ArrayList<>();
            activeDriversList.add(driver1);
            activeDriversList.add(student);
            activeDriversList.add(driver2);

            doReturn(activeDriversList).when(repository).findAllByStatus(GeneralStatus.ACTIVE);

            List<DriverResponseDTO> result = driverService.getAllActiveDrivers();

            DriverResponseDTO expectedDto1 = createdExpectedDTO(driver1);
            DriverResponseDTO expectedDto2 = createdExpectedDTO(driver2);

            assertEquals(2, result.size());

            assertEquals(expectedDto1.name(), result.getFirst().name());
            assertEquals(expectedDto2.name(), result.getLast().name());

            verify(repository).findAllByStatus(GeneralStatus.ACTIVE);
        }

        @DisplayName("Deve retornar uma lista vazia caso não haja registros no banco")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            // "ACT" PARA RETORNAR LISTA VAZIA
            doReturn(Collections.emptyList()).when(repository).findAllByStatus(GeneralStatus.ACTIVE);

            List<DriverResponseDTO> result = driverService.getAllActiveDrivers();

            assertEquals(0, result.size());

            verify(repository).findAllByStatus(GeneralStatus.ACTIVE);
        }
    }

    @Nested
    class getAllInactiveDrivers {

        @DisplayName("Deve retornar todos os motoristas com sucesso")
        @Test
        void getAllInactiveDriversSuccessfully() {
            List<UserModel> allInactiveDrivers = new ArrayList<>();
            allInactiveDrivers.add(driver1);
            allInactiveDrivers.add(student);
            allInactiveDrivers.add(driver2);

            doReturn(allInactiveDrivers).when(repository).findAllByStatus(GeneralStatus.INACTIVE);

            List<DriverResponseDTO> result = driverService.getAllInactiveDrivers();

            DriverResponseDTO expectedDriver1 = createdExpectedDTO(driver1);
            DriverResponseDTO expectedDriver2 = createdExpectedDTO(driver2);

            assertEquals(expectedDriver1.name(), result.getFirst().name());
            assertEquals(expectedDriver2.name(), result.get(1).name());

            verify(repository).findAllByStatus(GeneralStatus.INACTIVE);
        }

        @DisplayName("Deve retornar uma lista vazia caso não haja registros no banco")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            // "ACT" PARA RETORNAR LISTA VAZIA
            doReturn(Collections.emptyList()).when(repository).findAllByStatus(GeneralStatus.INACTIVE);

            List<DriverResponseDTO> result = driverService.getAllInactiveDrivers();

            assertEquals(0, result.size());

            verify(repository).findAllByStatus(GeneralStatus.INACTIVE);
        }
    }

    @Nested
    class createDriver {
        @DisplayName("Deve criar um motorista com sucesso")
        @Test
        void shouldCreateDriverWithSuccess() {
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "akjssd323",
                    "Iago",
                    null,
                    "75987435984",
                    null,
                    "city"
            );

            doReturn(Optional.empty()).when(repository).findByEmail(anyString());
            doReturn(Optional.empty()).when(repository).findByTelephone(anyString());

            doReturn("encoded_password_123").when(passwordEncoder).encode("akjssd323");

            doAnswer(invocation -> {
                Driver savedDriver = invocation.getArgument(0);
                assertEquals("encoded_password_123", savedDriver.getPassword());
                savedDriver.setId(UUID.randomUUID());
                return savedDriver;
            }).when(repository).save(any(Driver.class));

            DriverResponseDTO result = driverService.createDriver(dto);

            assertNotNull(result);
            assertNotNull(result.id(), "ID should be not null");
            assertEquals("Iago@gmail.com", result.email());
            assertEquals("Iago", result.name());
            assertEquals("75987435984", result.telephone());
            assertEquals("city", result.areaOfActivity());

            verify(repository).findByEmail(dto.email());
            verify(repository).findByTelephone(dto.telephone());

            verify(passwordEncoder).encode("akjssd323");

            verify(repository).save(any(Driver.class));
        }

        @DisplayName("Deve lançar exceção quando alguns campos são null")
        @Test
        void throwExceptionWhenFieldsIsNull() {
            DriverRequestDTO dto = new DriverRequestDTO(
                    null,
                    null,
                    "firstName",
                    "lastName",
                    null,
                    "pictureExemple",
                    null
            );

            assertThrows(RuntimeException.class, () -> driverService.createDriver(dto));

            verify(repository, never()).findByEmail(anyString());
            verify(repository, never()).findByTelephone(anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(repository, never()).save(any(Driver.class));
        }

        @DisplayName("Deve lançar exceção quando o email já existir no banco")
        @Test
        void throwExceptionWhenEmailAlreadyExists() {
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "teste",
                    "firstName",
                    "lastName",
                    "null",
                    "pictureExemple",
                    "null"
            );

            UserModel existingUser = new Driver();

            doReturn(Optional.of(existingUser)).when(repository).findByEmail(dto.email());
            doReturn(Optional.empty()).when(repository).findByTelephone(dto.telephone());

            RuntimeException throwns = assertThrows(RuntimeException.class, () -> driverService.createDriver(dto));

            assertEquals("Email já existe", throwns.getMessage());

            verify(repository).findByEmail(dto.email());
            verify(repository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o telefone já existir no banco de dados")
        @Test
        void throwExceptionWhenTelephoneAlreadyExists() {
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "teste",
                    "firstName",
                    "lastName",
                    "12809182",
                    "pictureExemple",
                    "null"
            );

            UserModel existingUser = new Driver();

            doReturn(Optional.empty()).when(repository).findByEmail(dto.email());
            doReturn(Optional.of(existingUser)).when(repository).findByTelephone(dto.telephone());

            RuntimeException thrown = assertThrows(RuntimeException.class, () -> driverService.createDriver(dto));

            assertEquals("Telefone já existe", thrown.getMessage());

            verify(repository).findByTelephone(dto.telephone());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class updateLoggedDriver {
        @DisplayName("Deve atualizar os campos de um motorista logado com sucesso")
        @Test
        void shouldUpdateLoggedDriverWithSuccess() {
            driver1.setStatus(GeneralStatus.ACTIVE);
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "akjssd323",
                    "Iago",
                    null,
                    "75987435984",
                    null,
                    "city"
            );

            when(repository.findByEmail(anyString())).thenReturn(Optional.of(driver1));
            when(repository.findByEmailOrTelephoneAndIdNot(anyString(), anyString(), any(UUID.class)))
                    .thenReturn(Optional.empty());

            doAnswer(invocation -> {
                driver1 = invocation.getArgument(0);
                return driver1;
            }).when(repository).save(any(Driver.class));

            DriverResponseDTO result = driverService.updateLoggedDriver(driver1.getEmail(), dto);

            verify(repository, times(1)).save(driver1);

            assertEquals(dto.email(), result.email());
            assertEquals(dto.name(), result.name());
            assertEquals(dto.telephone(), result.telephone());
            assertEquals(dto.areaOfActivity(), result.areaOfActivity());
        }

        @DisplayName("Deve lançar exceção quando email já estiver em uso por outro usuário")
        @Test
        void throwExceptionWhenEmailAlreadyExists() {
            driver1.setStatus(GeneralStatus.ACTIVE);
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "akjssd323",
                    "Iago",
                    null,
                    "75987435984",
                    null,
                    "city"
            );

            when(repository.findByEmail(anyString())).thenThrow(RuntimeException.class);

            assertThrows(RuntimeException.class, () -> {
                driverService.updateLoggedDriver(dto.email(), dto);
            });

            verify(repository).findByEmail(dto.email());
            verify(repository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o Status for INACTIVE")
        @Test
        void throwExceptionWhenIsInactiveUser() {
            driver1.setStatus(GeneralStatus.INACTIVE);

            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "akjssd323",
                    "Iago",
                    null,
                    "75987435984",
                    null,
                    "city"
            );

            assertThrows(RuntimeException.class, () -> {
                driverService.updateLoggedDriver(dto.email(), dto);
            });

            verify(repository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando email ou telefone já estiverem em ouso por outro user")
        @Test
        void throwExceptionWhenEmailOrTelephoneAlreadyExists() {
            driver1.setStatus(GeneralStatus.ACTIVE);
            DriverRequestDTO dto = new DriverRequestDTO(
                    "Iago@gmail.com",
                    "akjssd323",
                    "Iago",
                    null,
                    "75987435984",
                    null,
                    "city"
            );

            when(repository.findByEmail(dto.email())).thenReturn(Optional.of(driver1));
            when(repository.findByEmailOrTelephoneAndIdNot(dto.email(), dto.telephone(), driver1.getId()))
                    .thenReturn(Optional.of(driver1));

            assertThrows(RuntimeException.class, () -> {
                driverService.updateLoggedDriver(dto.email(), dto);
            });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class getLoggedInDriverProfile {

        @DisplayName("Deve retornar o motorista logado com sucesso")
        @Test
        void shouldGetLoggedInDriverProfile() {
            when(repository.findByEmailOrTelephone(driver1.getEmail(), driver1.getTelephone()))
                    .thenReturn(Optional.of(driver1));

            DriverResponseDTO result = driverService.getLoggedInDriverProfile(driver1.getEmail(), driver1.getTelephone());

            assertEquals(driver1.getEmail(), result.email());
            assertEquals(driver1.getTelephone(), result.telephone());
            assertNotNull(result);
        }

        @DisplayName("Deve lançar exceção quando motorista logado não for encontrado")
        @Test
        void throwExceptionWhenLoggedDriverIsNotWanted() {
            when(repository.findByEmailOrTelephone(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,  () -> {
                driverService.getLoggedInDriverProfile(driver1.getEmail(), driver1.getTelephone());
            });
        }
    }

    @Nested
    class disableDriver {

        @DisplayName("Deve desativar um motorista com sucesso")
        @Test
        void shouldDisableDriverWithSuccess() {
            driver1.setStatus(GeneralStatus.ACTIVE);

            when(repository.findById(driver1.getId())).thenReturn(Optional.of(driver1));

            driverService.disableDriver(driver1.getId());

            verify(repository).save(any(Driver.class));
        }

        @DisplayName("Deve lançar exceção quando o motorista já estiver desativado")
        @Test
        void throwExceptionWhenDriverAlreadyInactive() {
            driver1.setStatus(GeneralStatus.INACTIVE);

            when(repository.findById(driver1.getId())).thenReturn(Optional.of(driver1));

            assertThrows(RuntimeException.class, () -> {
                driverService.disableDriver(driver1.getId());
            });

            verify(repository, never()).save(any(Driver.class));
        }

        @DisplayName("Deve lançar exceção quando o id não é do motorista")
        @Test
        void throwExceptionWhenIsInvalidId() {
            driver1.setStatus(GeneralStatus.ACTIVE);

            when(repository.findById(any(UUID.class))).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                driverService.disableDriver(UUID.randomUUID());
            });

            verify(repository, never()).save(any(Driver.class));
        }

        @DisplayName("Deve lançar exceção quando o id do motorista não for encontrado")
        @Test
        void throwExceptionWhenNotWantedDriver() {
            driver1.setStatus(GeneralStatus.ACTIVE);

            when(repository.findById(driver1.getId())).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> {
                driverService.disableDriver(driver1.getId());
            });

            verify(repository, never()).save(any(Driver.class));
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