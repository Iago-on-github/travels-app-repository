package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.DuplicateResourceException;
import com.travel_system.backend_app.exceptions.EmptyMandatoryFieldsFound;
import com.travel_system.backend_app.model.Driver;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.StudentResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.model.enums.InstitutionType;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.UserModelRepository;
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

    private Driver driver;
    private Student student1;
    private Student student2;

    @BeforeEach
    void setUp() {
        student1 = createNewStudentForTesting(UUID.randomUUID(), "Student1", "Teste", "Student1@gmail.com", "24367824", LocalDateTime.now(), InstitutionType.UNIVERSITY, "CC", GeneralStatus.ACTIVE);
        student2= createNewStudentForTesting(UUID.randomUUID(), "Student2", "Teste", "Student2@gmail.com", "24546435", LocalDateTime.now(), InstitutionType.EXTENSION_COURSE, "Medicine", GeneralStatus.ACTIVE);
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