package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.*;
import com.travel_system.backend_app.infrastructure.TenantContext;
import com.travel_system.backend_app.interfaces.mappers.StudentRequestMapper;
import com.travel_system.backend_app.interfaces.mappers.response.StudentResponseMapper;
import com.travel_system.backend_app.model.Customer;
import com.travel_system.backend_app.model.Permissions;
import com.travel_system.backend_app.model.UserAccount;
import com.travel_system.backend_app.model.dtos.request.StudentUpdateDTO;
import com.travel_system.backend_app.model.enums.Shift;
import com.travel_system.backend_app.model.enums.UserAccountType;
import com.travel_system.backend_app.repository.CustomerRepository;
import com.travel_system.backend_app.repository.PermissionsRepository;
import com.travel_system.backend_app.repository.StudentRepository;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.dtos.request.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.response.StudentResponseDTO;
import com.travel_system.backend_app.model.enums.GeneralStatus;
import com.travel_system.backend_app.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final PermissionsRepository permissionsRepository;
    private final UserAccountRepository userAccountRepository;

    private final PasswordEncoder passwordEncoder;


    private final StudentResponseMapper studentResponseMapper;
    private final StudentRequestMapper studentRequestMapper;

    public StudentService(StudentRepository studentRepository, PermissionsRepository permissionsRepository, UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder, StudentResponseMapper studentResponseMapper, StudentRequestMapper studentRequestMapper) {
        this.studentRepository = studentRepository;
        this.permissionsRepository = permissionsRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.studentResponseMapper = studentResponseMapper;
        this.studentRequestMapper = studentRequestMapper;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponseDTO> getAllStudents() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Student> getAllStudents = studentRepository.findAll(pageable);

        return getAllStudents.map(studentResponseMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponseDTO> getStudentsByStatus(GeneralStatus status) {
        Pageable pageable = PageRequest.of(0, 10);

        if (status == null) status = GeneralStatus.ACTIVE;

        Page<Student> students = studentRepository.findAllByStatus(status, pageable);

        return students.map(studentResponseMapper::toDTO);
    }

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        verifyFieldsIsNull(requestDTO);

        if (userAccountRepository.existsByEmail(requestDTO.email())) {
            throw new DuplicateResourceException("O email " + requestDTO.email() + " já existe");
        }

        if (studentRepository.existsByTelephone(requestDTO.telephone())) {
            throw new DuplicateResourceException("O telefone " + requestDTO.telephone() + " já existe");
        }

/*        final String PERM = "ROLE_USER";
        Permissions studentPermission = permissionsRepository.findByDescription(PERM)
                .orElseThrow(() -> new PermissionNotFoundException("Permissão " + PERM + " não encontrada."));*/

        UserAccount userAccount = new UserAccount();
        userAccount.setPassword(passwordEncoder.encode(requestDTO.password()));
        userAccount.setEmail(requestDTO.email());
        userAccount.setPermissions(List.of());
        userAccount.setUserAccountType(UserAccountType.UNASSIGNED);

        UserAccount savedAccount = userAccountRepository.save(userAccount);

        Student student = studentRequestMapper.toEntity(requestDTO);

        student.setUserAccount(savedAccount);

        // vínculo com o(s) período(s)
        if (!requestDTO.studentShift().isEmpty()) {
            student.setStudentShift(requestDTO.studentShift());
        }

        Student savedStudent = studentRepository.save(student);

        return studentResponseMapper.toDTO(savedStudent);
    }

    @Transactional
    public StudentResponseDTO updateCurrentStudent(String authenticatedUserEmail, StudentUpdateDTO studentUpdateDTO) {
        Student studentEntity = studentRepository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado, " + authenticatedUserEmail));

        if (studentEntity.getStatus() == GeneralStatus.INACTIVE) {
          throw new InactiveAccountModificationException("Não é possível modificar dados de uma conta inativa: " + authenticatedUserEmail);
        }

        UserAccount userAccount = studentEntity.getUserAccount();

        // verifica se email já existe
        if (studentUpdateDTO.email() != null && !studentUpdateDTO.email().equals(userAccount.getEmail())) {
            if (userAccountRepository.existsByEmail(studentUpdateDTO.email())) {
                throw new DuplicateResourceException("Email já em uso por outro usuário.");
            }
        }

        // verifica se telefone já existe
        if (studentUpdateDTO.telephone() != null && !studentUpdateDTO.telephone().equals(studentEntity.getTelephone())) {
            if (studentRepository.existsByTelephone(studentUpdateDTO.telephone())) {
                throw new DuplicateResourceException("Telefone já em uso por outro usuário.");
            }
        }

        // atualiza parcialmente sempre ignorando a senha
        studentRequestMapper.studentUpdateFromDTO(studentUpdateDTO, studentEntity);

        // senha atualiza manualmente por conta do encrypt
        if (studentUpdateDTO.password() != null && !studentUpdateDTO.password().isBlank()) {
            userAccount.setPassword(passwordEncoder.encode(studentUpdateDTO.password()));
        }
        
        Student savedStudent = studentRepository.save(studentEntity);

        return studentResponseMapper.toDTO(savedStudent);
    }

    @Transactional(readOnly = true)
    public StudentResponseDTO getCurrentStudent(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrato: " + email));

        return studentResponseMapper.toDTO(student);
    }

    @Transactional
    public void updateStudentStatus(UUID studentId, GeneralStatus newStatus) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Estudante não encontrado, " + studentId));

        if (student.getStatus() == newStatus) {
            throw new DomainValidationException("Estudante " + studentId + " já com o status " + newStatus);
        }

        student.setStatus(newStatus);

        studentRepository.save(student);
    }

    private void verifyFieldsIsNull(StudentRequestDTO dto) {
        if (dto.email() == null || dto.password() == null ||
                dto.name() == null || dto.telephone() == null || dto.institutionType() == null || dto.course() == null) {
            throw new EmptyMandatoryFieldsFoundException("Você deve preencher todos os campos requeridos");
        }
    }

}
