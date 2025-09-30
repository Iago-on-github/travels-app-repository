package com.travel_system.backend_app.security;

import com.travel_system.backend_app.Repository.UserModelRepository;
import com.travel_system.backend_app.model.Student;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StudentUserDetailsService implements UserDetailsService {

    private final UserModelRepository userModelRepository;

    public StudentUserDetailsService(UserModelRepository userModelRepository) {
        this.userModelRepository = userModelRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        var user = userModelRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Um estudante não pôde ser encontrado com o email informado")
                );

        if (!(user instanceof Student)) {
            throw new UsernameNotFoundException("Usuário encontrado não é um estudante");
        }

        return new StudentUserDetails((Student) user);
    }
}
