package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.model.*;
import com.travel_system.backend_app.model.dtos.request.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.response.StudentResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    /*
    ====== ORGANIZAÇÃO DOS TESTES ======

    - Os testes de cada method devem ser isolados por classes anotadas com @nested
    - Em casos de muitos testes em uma mesma classe, criar classes específicas com @nested para cenários de
    success e throw exception.
    - Sempre usar @DisplayName para dar uma breve descrição do que aquele teste deve fazer
    - Em cenários de Success, os métodos devem conter "With Success" nos seus respetivos nomes.
    - Em cenários de Error, os métodos devem conter "throwExceptionWhen…" nos seus respetivos nomes.

    AS FASES DE CADA TESTE:

    - SETUP (CONFIGURAÇÃO INICIAL)
    - CENÁRIO DE SUCESSO (SUCCESS)
    - CENÁRIO DE ERRO (THROW EXCEPTION)
    */

    @InjectMocks
    private StudentService studentService;

    @Mock
    private UserModelRepository userModelRepository;
    @Mock
    private StudentTravelRepository studentTravelRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private StudentResponseDTO responseDTO = new StudentResponseDTO(
            UUID.randomUUID(),
            "Student Test",
            "Student Last-name",
            "Student@gmail.com",
            "736483643",
            LocalDateTime.now(),
            InstitutionType.UNIVERSITY,
            "Computer Science",
            GeneralStatus.ACTIVE
    );

    private Driver driver = new Driver();
    private Travel travel = new Travel();
    private StudentTravel studentTravel = new StudentTravel();
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        student1 = createNewStudentForTesting(UUID.randomUUID(), "Student1", "Teste", "Student1@gmail.com", "24367824", LocalDateTime.now(), InstitutionType.UNIVERSITY, "CC", GeneralStatus.ACTIVE);
        student2 = createNewStudentForTesting(UUID.randomUUID(), "Student2", "Teste", "Student2@gmail.com", "24546435", LocalDateTime.now(), InstitutionType.EXTENSION_COURSE, "Medicine", GeneralStatus.ACTIVE);

    }

    @Nested
    class getAllStudents {

        @DisplayName("Deve retornar todas as instâncias de Students com successo")
        @Test
        void getAllStudentsWithSuccess(){
            List<UserModel> mixedStudents = new ArrayList<>();
            mixedStudents.add(student1);
            mixedStudents.add(driver);
            mixedStudents.add(student2);

            doReturn(mixedStudents)
                    .when(userModelRepository)
                    .findAll();

            List<StudentResponseDTO> result = studentService.getAllStudents();

            StudentResponseDTO expectedResponseDto1 = expectedResponseDto(student1);
            StudentResponseDTO expectedResponseDto2 = expectedResponseDto(student2);

            assertEquals(2, result.size());

            assertEquals(expectedResponseDto1.name(), result.getFirst().name());
            assertEquals(expectedResponseDto2.email(), result.getLast().email());

            verify(userModelRepository).findAll();
        }

        @DisplayName("Deve retornar uma lista vazia quando não encontrar registros")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            doReturn(Collections.emptyList()).when(userModelRepository).findAll();

            List<StudentResponseDTO> result = studentService.getAllStudents();

            assertEquals(0, result.size());

            verify(userModelRepository).findAll();
        }
    }

    @Nested
    class getAllActiveStudents {

        @DisplayName("Deve retornar todos os estudantes ativos com sucesso")
        @Test
        void shouldReturnAllActiveStudentsWithSuccess() {
            List<UserModel> activeStudentsList = new ArrayList<>();
            activeStudentsList.add(student1);
            activeStudentsList.add(driver);
            activeStudentsList.add(student2);

            doReturn(activeStudentsList).when(userModelRepository).findAllByStatus(GeneralStatus.ACTIVE);

            List<StudentResponseDTO> result = studentService.getAllActiveStudents();

            StudentResponseDTO expectedDto1 = expectedResponseDto(student1);
            StudentResponseDTO expectedDto2 = expectedResponseDto(student2);

            assertEquals(2, result.size());

            assertEquals(expectedDto1.name(), result.getFirst().name());
            assertEquals(expectedDto2.name(), result.getLast().name());

            verify(userModelRepository).findAllByStatus(GeneralStatus.ACTIVE);
        }

        @DisplayName("Deve retornar uma lista vazia quando não houver registros de estudantes ativos")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            doReturn(Collections.emptyList()).when(userModelRepository).findAllByStatus(GeneralStatus.ACTIVE);

            List<StudentResponseDTO> result = studentService.getAllActiveStudents();

            assertEquals(0, result.size());

            verify(userModelRepository).findAllByStatus(GeneralStatus.ACTIVE);
        }
    }

    @Nested
    class getAllInactiveStudents {
        @DisplayName("Deve retornar todos os estudantes inativos com sucesso")
        @Test
        void shouldReturnAllInactiveStudentsWithSuccess() {
            student1.setStatus(GeneralStatus.INACTIVE);
            student2.setStatus(GeneralStatus.INACTIVE);

            List<UserModel> inactiveStudentList = new ArrayList<>();
            inactiveStudentList.add(student1);
            inactiveStudentList.add(driver);
            inactiveStudentList.add(student2);

            doReturn(inactiveStudentList).when(userModelRepository).findAllByStatus(GeneralStatus.INACTIVE);

            List<StudentResponseDTO> result = studentService.getAllInactiveStudents();

            StudentResponseDTO expectedDto1 = expectedResponseDto(student1);
            StudentResponseDTO expectedDto2 = expectedResponseDto(student2);

            assertEquals(2, result.size());

            assertEquals(expectedDto1.name(), result.getFirst().name());
            assertEquals(expectedDto2.name(), result.getLast().name());

            verify(userModelRepository).findAllByStatus(GeneralStatus.INACTIVE);
        }

        @DisplayName("Deve retornar uma lista vazia quando não houver registros")
        @Test
        void shouldReturnAnEmptyListWithSuccess() {
            doReturn(Collections.emptyList()).when(userModelRepository).findAllByStatus(GeneralStatus.INACTIVE);

            List<StudentResponseDTO> result = studentService.getAllInactiveStudents();

            assertEquals(0, result.size());

            verify(userModelRepository).findAllByStatus(GeneralStatus.INACTIVE);
        }
    }

    @Nested
    class createStudent {

        @DisplayName("Deve criar um estudante com sucesso")
        @Test
        void shouldCreateStudentWithSuccess() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "profile_picture_example",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            doReturn(Optional.empty()).when(userModelRepository).findByEmail(anyString());
            doReturn(Optional.empty()).when(userModelRepository).findByTelephone(anyString());

            doReturn("encoded_password").when(passwordEncoder).encode(requestDto.password());

            doAnswer(invocation -> {
                Student savedStudent = invocation.getArgument(0);
                assertEquals("encoded_password", savedStudent.getPassword());
                savedStudent.setId(UUID.randomUUID());
                return savedStudent;
            }).when(userModelRepository).save(any(Student.class));

            StudentResponseDTO result = studentService.createStudent(requestDto);

            assertNotNull(result);
            assertNotNull(result.id());

            assertEquals(requestDto.email(), result.email());
            assertEquals(requestDto.name(), result.name());
            assertEquals(requestDto.institutionType(), result.institutionType());

            verify(userModelRepository).findByEmail(requestDto.email());
            verify(userModelRepository).findByTelephone(requestDto.telephone());

            verify(passwordEncoder).encode(requestDto.password());

            verify(userModelRepository).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando há campos requeridos nulos")
        @Test
        void shouldThrowExceptionWhenFieldsIsNull() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    null,
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    null,
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            assertThrows(EmptyMandatoryFieldsFound.class, () -> {
                studentService.createStudent(requestDto);
            });

            verify(userModelRepository, never()).findByEmail(anyString());
            verify(userModelRepository, never()).findByTelephone(anyString());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userModelRepository, never()).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando email já estiver em uso")
        @Test
        void throwExceptionWhenEmailAlreadyExists() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "teste",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            UserModel existingStudent = new Student();

            doReturn(Optional.of(existingStudent)).when(userModelRepository).findByEmail(requestDto.email());
            doReturn(Optional.empty()).when(userModelRepository).findByTelephone(requestDto.telephone());

            DuplicateResourceException error = assertThrows(DuplicateResourceException.class, () -> {
                studentService.createStudent(requestDto);
            });

            assertEquals("O email" + ", " + requestDto.email() + ", " + "já existe", error.getMessage());

            verify(userModelRepository).findByEmail(requestDto.email());
            verify(userModelRepository, never()).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando telefone já estiver em uso")
        @Test
        void throwExceptionWhenTelephoneAlreadyExists() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "teste",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            UserModel existingStudent = new Student();

            doReturn(Optional.of(existingStudent)).when(userModelRepository).findByTelephone(anyString());
            doReturn(Optional.empty()).when(userModelRepository).findByEmail(anyString());

            DuplicateResourceException error = assertThrows(DuplicateResourceException.class, () -> {
                studentService.createStudent(requestDto);
            });

            assertEquals("O telefone" + ", " + requestDto.telephone() + ", " + "já existe", error.getMessage());

            verify(userModelRepository).findByTelephone(anyString());
            verify(userModelRepository, never()).save(any(Student.class));
        }
    }

    @Nested
    class updateLoggedStudent {

        @DisplayName("Deve atualizar um estudante logado com sucesso")
        @Test
        void shouldUpdateLoggedStudentWithSuccess() {
            student1.setStatus(GeneralStatus.ACTIVE);
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "profile_picture_example",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            when(userModelRepository.findByEmail(anyString())).thenReturn(Optional.of(student1));
            when(userModelRepository.findByEmailOrTelephoneAndIdNot(anyString(), anyString(), any(UUID.class))).thenReturn(Optional.empty());

            doAnswer(invocation -> {
                student1 = invocation.getArgument(0);
                return student1;
            }).when(userModelRepository).save(any(Student.class));

            StudentResponseDTO result = studentService.updateLoggedStudent(student1.getEmail(), requestDto);

            verify(userModelRepository, times(1)).save(any(Student.class));

            assertEquals(requestDto.email(), result.email());
            assertEquals(requestDto.name(), result.name());
            assertEquals(requestDto.telephone(), result.telephone());
        }

        @DisplayName("Deve lançar exceção quando o email já estiver em uso por outro usuário")
        @Test
        void throwExceptionWhenEmailAlreadyUsed() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "profile_picture_example",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            when(userModelRepository.findByEmail(anyString())).thenThrow(EntityNotFoundException.class);

            assertThrows(EntityNotFoundException.class, () -> {
                studentService.updateLoggedStudent(requestDto.email(), requestDto);
            });

            verify(userModelRepository).findByEmail(anyString());
            verify(userModelRepository, never()).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando o estudante for inativo")
        @Test
        void throwExceptionWhenIsInactiveUser() {
            student1.setStatus(GeneralStatus.INACTIVE);

            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "profile_picture_example",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            when(userModelRepository.findByEmail(anyString())).thenThrow(InactiveAccountModificationException.class);

            assertThrows(InactiveAccountModificationException.class, () -> {
                studentService.updateLoggedStudent(requestDto.email(), requestDto);
            });

            verify(userModelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando email ou telefone já estiver em uso por outro usuário")
        @Test
        void throwExceptionWhenEmailOrTelephoneAlreadyUsed() {
            StudentRequestDTO requestDto = new StudentRequestDTO(
                    "teste@gmail.com",
                    "exemple_encoded_pass",
                    "teste01",
                    "last_name_teste",
                    "8642876424",
                    "profile_picture_example",
                    InstitutionType.UNIVERSITY,
                    "example_course"
            );

            when(userModelRepository.findByEmail(anyString())).thenReturn(Optional.of(student1));
            when(userModelRepository.findByEmailOrTelephoneAndIdNot(anyString(), anyString(), any(UUID.class))).thenReturn(Optional.of(student1));

            assertThrows(DuplicateResourceException.class, () ->{
                studentService.updateLoggedStudent(requestDto.email(), requestDto);
            });

            verify(userModelRepository).findByEmailOrTelephoneAndIdNot(anyString(), anyString(), any(UUID.class));
            verify(userModelRepository).findByEmail(anyString());

            verify(userModelRepository, never()).save(any());
        }
    }

    @Nested
    class getLoggedInStudentProfile {

        @DisplayName("Deve retornar um estudante logado com sucesso")
        @Test
        void shouldGetLoggedInStudentProfileWithSuccess() {
            when(userModelRepository.findByEmailOrTelephone(student1.getEmail(), student1.getTelephone())).thenReturn(Optional.of(student1));

            StudentResponseDTO result = studentService.getLoggedInStudentProfile(student1.getEmail(), student1.getTelephone());

            assertNotNull(result);

            assertEquals(student1.getEmail(), result.email());
            assertEquals(student1.getTelephone(), result.telephone());
        }

        @DisplayName("Deve lançar exceção quando o estudante logado não for encontrado")
        @Test
        void throwExceptionWhenLoggedStudentIsNotWanted() {
            when(userModelRepository.findByEmailOrTelephone(student1.getEmail(), student1.getTelephone()))
                    .thenReturn(Optional.empty());

            EntityNotFoundException error = assertThrows(EntityNotFoundException.class, () -> {
                studentService.getLoggedInStudentProfile(student1.getEmail(), student1.getTelephone());
            });

            assertEquals("Estudante não encontrato", error.getMessage());

        }
    }

    @Nested
    class disableStudent {

        @DisplayName("Deve desativar um estudante com sucesso")
        @Test
        void shouldDisableStudentWithSuccess() {
            when(userModelRepository.findById(student1.getId())).thenReturn(Optional.of(student1));

            studentService.disableStudent(student1.getId());

            verify(userModelRepository).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando o estudante não for encontrado")
        @Test
        void throwExceptionWhenStudentNotFound() {
            when(userModelRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> {
                studentService.disableStudent(student1.getId());
            });

            verify(userModelRepository, never()).save(any(Student.class));
        }

        @DisplayName("Deve lançar exceção quando o estudante já estiver desativado")
        @Test
        void throwExceptionWhenStudentAlreadyInactive() {
            student1.setStatus(GeneralStatus.INACTIVE);

            when(userModelRepository.findById(student1.getId())).thenReturn(Optional.of(student1));

            assertThrows(IllegalStateException.class, () -> {
                studentService.disableStudent(student1.getId());
            });

            verify(userModelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o id não for de um estudante")
        @Test
        void throwExceptionWhenUserNotIsStudent() {
            UUID id = UUID.randomUUID();

            driver.setId(id);
            driver.setStatus(GeneralStatus.ACTIVE);

            when(userModelRepository.findById(id)).thenReturn(Optional.of(driver));

            assertThrows(IllegalArgumentException.class, () -> {
                studentService.disableStudent(id);
            });

            verify(userModelRepository, never()).save(any());
        }
    }

    @Nested
    class confirmEmbarkOnTravel {

        @DisplayName("Deve confirmar o embarque o estudante com sucesso")
        @Test
        void shouldConfirmEmbarkOnTravelWithSuccess() {
            studentTravel.setEmbark(false);

            when(studentTravelRepository.findByStudentIdAndTravelId(student1.getId(), travel.getId()))
                    .thenReturn(Optional.of(studentTravel));

            studentService.confirmEmbarkOnTravel(student1.getId(), travel.getId());

            verify(studentTravelRepository).save(any(StudentTravel.class));
        }

        @DisplayName("Deve lançar exceção quando não haver relacionamento travel > estudante")
        @Test
        void throwExceptionWhenTravelStudentAssociationNotFound() {
            studentTravel.setEmbark(false);

            when(studentTravelRepository.findByStudentIdAndTravelId(student1.getId(), driver.getId()))
                    .thenReturn(Optional.empty());

            assertThrows(TravelStudentAssociationNotFoundException.class, () -> {
                studentService.confirmEmbarkOnTravel(student1.getId(), travel.getId());
            });

            verify(studentTravelRepository, never()).save(any());
        }

        @DisplayName("Deve lançar exceção quando o estudante já havia confirmado embarque")
        @Test
        void throwExceptionWhenEmbarkAlreadyConfirmed() {
            studentTravel.setEmbark(true);

            when(studentTravelRepository.findByStudentIdAndTravelId(student1.getId(), travel.getId()))
                    .thenThrow(BoardingAlreadyConfirmedException.class);

            assertThrows(BoardingAlreadyConfirmedException.class, () -> {
                studentService.confirmEmbarkOnTravel(student1.getId(), travel.getId());
            });

            verify(studentTravelRepository, never()).save(any());
        }
    }


    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES

    private StudentResponseDTO expectedResponseDto(Student student) {
        return new StudentResponseDTO(
                student.getId(),
                student.getName(),
                student.getLastName(),
                student.getEmail(),
                student.getTelephone(),
                student.getCreatedAt(),
                student.getInstitutionType(),
                student.getCourse(),
                student.getStatus()
        );
    }

    private Student createNewStudentForTesting(UUID id, String name, String lastName, String email, String telephone, LocalDateTime createdAt, InstitutionType institutionType, String course, GeneralStatus status) {
        Student student = new Student();

        student.setId(id);
        student.setName(name);
        student.setLastName(lastName);
        student.setEmail(email);
        student.setTelephone(telephone);
        student.setCreatedAt(createdAt);
        student.setInstitutionType(institutionType);
        student.setCourse(course);
        student.setStatus(status);

        return student;
    }
}