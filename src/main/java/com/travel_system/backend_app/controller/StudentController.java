package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.model.dtos.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.StudentResponseDTO;
import com.travel_system.backend_app.service.StudentService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/students/api/v1")
public class StudentController {
    private StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping()
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        return ResponseEntity.ok().body(studentService.getAllStudents());
    }

    @GetMapping("/actives")
    public ResponseEntity<List<StudentResponseDTO>> getAllActiveStudents() {
        return ResponseEntity.ok().body(studentService.getAllActiveStudents());
    }

    @GetMapping("/inactives")
    public ResponseEntity<List<StudentResponseDTO>> getAllInactiveStudents() {
        return ResponseEntity.ok().body(studentService.getAllInactiveStudents());
    }

    @GetMapping("/{email}/{telephone}")
    public ResponseEntity<StudentResponseDTO> getLoggedInStudentProfile(@PathVariable String email, @PathVariable String telephone) {
        return ResponseEntity.ok().body(studentService.getLoggedInStudentProfile(email, telephone));
    }

    @PostMapping
    public ResponseEntity<StudentResponseDTO> createStudent(@RequestBody StudentRequestDTO studentRequestDTO, UriComponentsBuilder componentsBuilder) {
        StudentResponseDTO student = studentService.createStudent(studentRequestDTO);
        URI uri = componentsBuilder.path("{/id}").buildAndExpand(student.id()).toUri();
        return ResponseEntity.created(uri).body(student);
    }

    @PutMapping("/{authenticatedUserEmail}")
    public ResponseEntity<StudentResponseDTO> updateStudent(@PathVariable String authenticatedUserEmail, @RequestBody StudentRequestDTO studentRequestDTO) {
        return ResponseEntity.ok().body(studentService.updateLoggedStudent(authenticatedUserEmail, studentRequestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> disableStudent(@PathVariable UUID id) {
        studentService.disableStudent(id);
        return ResponseEntity.ok().build();
    }
}
