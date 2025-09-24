package com.travel_system.backend_app.Service;

import com.travel_system.backend_app.Repository.UserModelRepository;
import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.model.dtos.StudentResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        return getAllStudents.stream()
                .filter(student -> student instanceof Student)
                .map(user -> studentConverted((Student) user)).toList();
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
