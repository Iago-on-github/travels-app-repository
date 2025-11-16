package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.model.StudentTravel;
import com.travel_system.backend_app.repository.StudentTravelRepository;
import com.travel_system.backend_app.repository.UserModelRepository;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.StudentResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
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
public class StudentService {
    private UserModelRepository repository;
    private StudentTravelRepository studentTravelRepository;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public StudentService(UserModelRepository repository, StudentTravelRepository studentTravelRepository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.studentTravelRepository = studentTravelRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // na tabela 'userModel', chama apenas instancias de Student
    public List<StudentResponseDTO> getAllStudents() {
        List<UserModel> getAllStudents = repository.findAll();

        if (getAllStudents.isEmpty()) return Collections.emptyList();

        return getAllStudents.stream()
                .filter(student -> student instanceof Student)
                .map(user -> studentConverted((Student) user)).toList();
    }

    public List<StudentResponseDTO> getAllActiveStudents() {
        return getStudentsByStatus(GeneralStatus.ACTIVE);
    }

    public List<StudentResponseDTO> getAllInactiveStudents() {
        return getStudentsByStatus(GeneralStatus.INACTIVE);
    }

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        Student newStudent = studentMapper(requestDTO);

        verifyFieldsIsNull(requestDTO);

        // cryptography the password
        String rawPassword = newStudent.getPassword();
        String encodedPassword = passwordEncoder.encode(rawPassword);
        newStudent.setPassword(encodedPassword);

        Optional<UserModel> email = repository.findByEmail(newStudent.getEmail());
        Optional<UserModel> telephone = repository.findByTelephone(newStudent.getTelephone());

        if (email.isPresent()) throw new DuplicateResourceException("O email" + ", " + requestDTO.email() + ", " + "já existe");
        if (telephone.isPresent()) throw new DuplicateResourceException("O telefone" + ", " + requestDTO.telephone() + ", " + "já existe");

        Student savedStudent = repository.save(newStudent);
        return studentConverted(savedStudent);
    }

    @Transactional
    public StudentResponseDTO updateLoggedStudent(String authenticatedUserEmail, StudentRequestDTO requestDTO) {
        Student existingStudent = (Student) repository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado, " + authenticatedUserEmail));

        if (existingStudent.getStatus().equals(GeneralStatus.INACTIVE)) {
          throw new InactiveAccountModificationException("Não é possível modificar dados de uma conta inativa.");
        }

        // verifica se email/tel/id ja existe no banco
        Optional<UserModel> existingUser = repository.findByEmailOrTelephoneAndIdNot(
                requestDTO.email(),
                requestDTO.telephone(),
                existingStudent.getId()
        );
        if (existingUser.isPresent()) throw new DuplicateResourceException("Email ou telefone já em uso por outro usuário.");

        existingStudent.setEmail(requestDTO.email());
        existingStudent.setPassword(requestDTO.password());
        existingStudent.setName(requestDTO.name());
        existingStudent.setLastName(requestDTO.lastName());
        existingStudent.setTelephone(requestDTO.telephone());
        existingStudent.setProfilePicture(requestDTO.profilePicture());
        existingStudent.setInstitutionType(requestDTO.institutionType());
        existingStudent.setCourse(requestDTO.course());

        Student savedStudent = repository.save(existingStudent);
        return studentConverted(savedStudent);
    }

    public StudentResponseDTO getLoggedInStudentProfile(String email, String telephone) {
        Student student = (Student) repository.findByEmailOrTelephone(email, telephone)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrato"));

        return studentConverted(student);
    }

    @Transactional
    public void disableStudent(UUID id) {
        Optional<UserModel> student = repository.findById(id);
        UserModel userModel = student.orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado, " + id));

        if (userModel instanceof Student studentRequest) {
            if (studentRequest.getStatus().equals(GeneralStatus.INACTIVE)) {
                throw new IllegalStateException("Estudante já desativado, " + id);
            }
            studentRequest.setStatus(GeneralStatus.INACTIVE);
            repository.save(studentRequest);
        } else {
            throw new IllegalArgumentException("Usuário não é um estudante, " + id);
        }
    }

    // haverá um popup no front que perguntará se o estudante irá participar da viagem
    public void confirmEmbarkOnTravel(UUID studentId, UUID travelId) {
        StudentTravel studentTravel = studentTravelRepository
                .findByStudentIdAndTravelId(studentId, travelId)
                .orElseThrow(() -> new TravelStudentAssociationNotFoundException("Associação travel e student não encontrada"));

        if (studentTravel.isEmbark()) {
            throw new BoardingAlreadyConfirmedException("Embarque já confirmado");
        }

        studentTravel.setEmbark(true);
        studentTravelRepository.save(studentTravel);
    }

    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES
    // MÉTODOS AUXILIARES

    private List<StudentResponseDTO> getStudentsByStatus(GeneralStatus status) {
        List<UserModel> students = repository.findAllByStatus(status);
        if (students.isEmpty()) Collections.emptyList();
        return students.stream().filter(student -> student instanceof Student)
                .map(student -> studentConverted((Student) student))
                .toList();
    }

    private Student studentMapper(StudentRequestDTO requestDTO) {
        Student newStudent = new Student();

        newStudent.setEmail(requestDTO.email());
        newStudent.setPassword(requestDTO.password());
        newStudent.setName(requestDTO.name());
        newStudent.setLastName(requestDTO.lastName());
        newStudent.setTelephone(requestDTO.telephone());
        newStudent.setProfilePicture(requestDTO.profilePicture());
        newStudent.setInstitutionType(requestDTO.institutionType());
        newStudent.setCourse(requestDTO.course());

        return newStudent;
    }

    private StudentResponseDTO studentConverted(Student student) {
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

    private void verifyFieldsIsNull(StudentRequestDTO dto) {
        if (dto.email() == null || dto.password() == null ||
                dto.name() == null || dto.telephone() == null || dto.institutionType() == null || dto.course() == null) {
            throw new EmptyMandatoryFieldsFound("Você deve preencher todos os campos requeridos");
        }
    }

}
