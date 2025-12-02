package com.travel_system.backend_app.security;

import com.travel_system.backend_app.config.TokenConfig;
import com.travel_system.backend_app.exceptions.EmailNotFoundException;
import com.travel_system.backend_app.model.dtos.request.LoginRequestDTO;
import com.travel_system.backend_app.model.dtos.response.LoginResponseDTO;
import com.travel_system.backend_app.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, TokenConfig tokenConfig) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
    }

    public ResponseEntity signin(LoginRequestDTO loginRequestDto) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDto.email(), loginRequestDto.password()));

            var user = userRepository.findUserByEmail(loginRequestDto.email());

            if (user.isEmpty()){
                throw new EmailNotFoundException("Email ou senha inválidos. Tente novamente");
            }
// verificar a classe "user" e implementar os métodos faltantes de roles.

//            var tokenResponse = tokenConfig.createAccessToken(loginRequestDto.email(), user.);

        } catch (Exception e) {
            System.out.println(e.getCause());
        }
        return null;
    }
}
