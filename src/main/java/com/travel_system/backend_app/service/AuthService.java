package com.travel_system.backend_app.service;

import com.travel_system.backend_app.config.TokenConfig;
import com.travel_system.backend_app.exceptions.EmailNotFoundException;
import com.travel_system.backend_app.exceptions.PermissionNotFoundException;
import com.travel_system.backend_app.model.Administrator;
import com.travel_system.backend_app.model.Permissions;
import com.travel_system.backend_app.model.dtos.request.AdministratorProfileDTO;
import com.travel_system.backend_app.model.dtos.request.LoginRequestDTO;
import com.travel_system.backend_app.repository.PermissionsRepository;
import com.travel_system.backend_app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;
    private final PasswordEncoder passwordEncoder;
    private final PermissionsRepository permissionsRepository;

    public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager, TokenConfig tokenConfig, PasswordEncoder passwordEncoder, PermissionsRepository permissionsRepository) {
        this.userRepository = userRepository;
        this.authenticationManager = authenticationManager;
        this.tokenConfig = tokenConfig;
        this.passwordEncoder = passwordEncoder;
        this.permissionsRepository = permissionsRepository;
    }

    public ResponseEntity createAdministrator(AdministratorProfileDTO admProfileDto) {

        if (userRepository.findUserByEmail(admProfileDto.email()) != null ||
                userRepository.findUserByCpf(admProfileDto.cpf()) != null ) {
            return ResponseEntity.badRequest().body("Email ou Cpf já cadastrados.");
        }

        Administrator adm = new Administrator();
        adm.setName(admProfileDto.name());
        adm.setEmail(admProfileDto.email());
        adm.setCpf(admProfileDto.cpf());
        String passwordEncoded = passwordEncoder.encode(admProfileDto.password());
        adm.setPassword(passwordEncoded);

        final String ROLE_ADMIN = "ROLE_ADMIN";

        Permissions adminPerm = permissionsRepository.findByDescription(ROLE_ADMIN)
                .orElseThrow(() -> new PermissionNotFoundException("Permissão " + ROLE_ADMIN + " não encontrada."));

        adm.setPermissions(List.of(adminPerm));

        userRepository.save(adm);

        return ResponseEntity.ok().body("Administrador criado com sucesso.");
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity signing(LoginRequestDTO loginRequestDto) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequestDto.email(), loginRequestDto.password()));

            var user = userRepository.findUserByEmail(loginRequestDto.email());

            if (user == null){
                throw new EmailNotFoundException("Email não encontrado. Tente novamente");
            }

            var tokenResponse = tokenConfig.createAccessToken(loginRequestDto.email(), user.getRoles());
            return ResponseEntity.ok().body(tokenResponse);

        } catch (Exception e) {
            throw new BadCredentialsException("Email ou senha inválidos. Tente novamente");
        }
    }

    @SuppressWarnings("rawtypes")
    public ResponseEntity refreshToken(String email, String refreshToken) {
        var user = userRepository.findUserByEmail(email);

        if (user == null) throw new EmailNotFoundException("Email não encontrado. Tente novamente");

        var loginResponse = tokenConfig.refreshToken(refreshToken);

        return ResponseEntity.ok().body(loginResponse);
    }
}
