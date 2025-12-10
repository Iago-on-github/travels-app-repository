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
            "Computer Science"
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
                student.getCourse()
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