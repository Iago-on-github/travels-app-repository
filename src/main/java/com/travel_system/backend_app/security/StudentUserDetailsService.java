package com.travel_system.backend_app.security;

import com.travel_system.backend_app.model.Student;
import com.travel_system.backend_app.repository.StudentRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StudentUserDetailsService implements UserDetailsService {

    private StudentRepository studentRepository;

    public StudentUserDetailsService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Um estudante não pôde ser encontrado com o email informado"));


        return new StudentUserDetails(student);
    }
}
