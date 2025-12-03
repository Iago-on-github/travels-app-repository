package com.travel_system.backend_app.service;

import com.travel_system.backend_app.exceptions.EmailNotFoundException;
import com.travel_system.backend_app.model.UserModel;
import com.travel_system.backend_app.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws EmailNotFoundException {
        UserModel userByEmail = repository.findUserByEmail(username);

        if (username.isEmpty()) throw new EmailNotFoundException(username);

        return userByEmail;
    }
}
