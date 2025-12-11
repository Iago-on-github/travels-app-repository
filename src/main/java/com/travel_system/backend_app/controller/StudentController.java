package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.request.StudentRequestDTO;
import com.travel_system.backend_app.model.dtos.response.StudentResponseDTO;
import com.travel_system.backend_app.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/students/api")
public class StudentController {
    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public ResponseEntity<List<StudentResponseDTO>> getAllStudents() {
        return ResponseEntity.ok().body(studentService.getAllStudents());
    }

    @GetMapping("/active")
    public ResponseEntity<List<StudentResponseDTO>> getAllActiveStudents() {
        return ResponseEntity.ok().body(studentService.getAllActiveStudents());
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<StudentResponseDTO>> getAllInactiveStudents() {
        return ResponseEntity.ok().body(studentService.getAllInactiveStudents());
    }

    @GetMapping("/logged")
    public ResponseEntity<StudentResponseDTO> getLoggedInStudentProfile(Authentication auth) {
        String email = auth.getName();
        return ResponseEntity.ok().body(studentService.getLoggedInStudentProfile(email));
    }

    @PostMapping("/new")
    public ResponseEntity<StudentResponseDTO> createStudent(@RequestBody StudentRequestDTO studentRequestDTO, UriComponentsBuilder componentsBuilder) {
        StudentResponseDTO student = studentService.createStudent(studentRequestDTO);
        URI uri = componentsBuilder.path("{/id}").buildAndExpand(student.id()).toUri();
        return ResponseEntity.created(uri).body(student);
    }

    @PutMapping("/update")
    public ResponseEntity<StudentResponseDTO> updateLoggedStudent(Authentication auth, @RequestBody StudentRequestDTO studentRequestDTO) {
        String email = auth.getName();
        return ResponseEntity.ok().body(studentService.updateLoggedStudent(email, studentRequestDTO));
    }

    @PutMapping("/disable/{id}")
    public ResponseEntity<Void> disableStudent(@PathVariable UUID id) {
        studentService.disableStudent(id);
        return ResponseEntity.ok().build();
    }

}
