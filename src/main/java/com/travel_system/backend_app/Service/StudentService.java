package com.travel_system.backend_app.Service;

import com.travel_system.backend_app.Repository.UserModelRepository;
import com.travel_system.backend_app.customExceptions.NoStudentsFoundException;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.StudentResponseDTO;
import com.travel_system.backend_app.model.enums.Role;
import com.travel_system.backend_app.model.enums.StatusStudent;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StudentService {
    private UserModelRepository repository;

    @Autowired
    public StudentService(UserModelRepository repository) {
        this.repository = repository;
    }

    // na tabela 'userModel', chama apenas instancias de Student
    public List<StudentResponseDTO> getAllStudents() {
        List<UserModel> getAllStudents = repository.findAll();
        if (getAllStudents.isEmpty()) throw new NoStudentsFoundException("Estudantes não encontrados.");
        return getAllStudents.stream()
                .filter(student -> student instanceof Student)
                .map(user -> studentConverted((Student) user)).toList();
    }

    public List<StudentResponseDTO> getAllActiveStudents() {
        return getStudentsByStatus(StatusStudent.ACTIVE, "sem estudantes ativos.");
    }

    public List<StudentResponseDTO> getAllInactiveStudents() {
        return getStudentsByStatus(StatusStudent.INACTIVE, "sem estudantes inativos.");
    }

    @Transactional
    public StudentResponseDTO createStudent(StudentRequestDTO requestDTO) {
        Student newStudent = studentMapper(requestDTO);

        newStudent.setRole(Role.ROLE_USER);
        newStudent.setstatus(StatusStudent.ACTIVE);

        Optional<UserModel> email = repository.findByEmail(newStudent.getEmail());
        Optional<UserModel> telephone = repository.findByTelephone(newStudent.getTelephone());
        if (email.isPresent()) throw new RuntimeException("Email já existe");
        if (telephone.isPresent()) throw new RuntimeException("Telefone já existe");

        Student savedStudent = repository.save(newStudent);
        return studentConverted(savedStudent);
    }

    @Transactional
    public StudentResponseDTO updateLoggedStudent(UUID currentID, String authenticatedUserEmail, StudentRequestDTO requestDTO) {
        Student existingStudent = (Student) repository.findByEmail(authenticatedUserEmail)
                .orElseThrow(() -> new RuntimeException("Estudante não encontrado, " + authenticatedUserEmail));

        StatusStudent inactive = StatusStudent.INACTIVE;
        if (existingStudent.getstatus() == inactive) {
          throw new RuntimeException("Não é possível modificar dados de uma conta inativa.");
        }

        // verifica se email/tel/id ja existe no banco
        Optional<UserModel> existingUser = repository.findByEmailOrTelephoneAndIdNot(
                requestDTO.email(),
                requestDTO.telephone(),
                existingStudent.getId()
        );
        if (existingUser.isPresent()) throw new RuntimeException("Email ou telefone já em uso por outro usuário.");

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
        Student student = repository.findByEmailOrTelephone(email, telephone)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrato."));

        return studentConverted(student);
    }

    private List<StudentResponseDTO> getStudentsByStatus(StatusStudent status, String exceptionMessage) {
        List<Student> students = repository.findAllByStatus(status);
        if (students.isEmpty()) throw new NoStudentsFoundException(exceptionMessage);
        return students.stream().map(this::studentConverted).toList();
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
                student.getstatus()
        );
    }
}
