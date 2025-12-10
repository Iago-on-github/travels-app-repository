package com.travel_system.backend_app.controller;

import com.travel_system.backend_app.model.dtos.request.LoginRequestDTO;
import com.travel_system.backend_app.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signing")
    public ResponseEntity signing(@RequestBody LoginRequestDTO data) {
        if (checkParamsIsNotNull(data)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falta email ou senha.");

        var token = authService.signing(data);
        if (token == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falha na requisição do cliente.");

        return ResponseEntity.ok().body(token);
    }

    @PostMapping("/refresh")
    public ResponseEntity refreshToken(Authentication auth, @RequestHeader("Authorization") String refreshToken) {
        String email = auth.getName(); // email do user JÁ logado

        if (checkParamsIsNotNull(email, refreshToken)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falta email ou senha.");

        var token = authService.refreshToken(email, refreshToken);
        if (token == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Falha na requisição do cliente.");

        return ResponseEntity.ok().body(token);
    }

//    MÉTODOS AUXILIARES
//    MÉTODOS AUXILIARES
//    MÉTODOS AUXILIARES

    private boolean checkParamsIsNotNull(LoginRequestDTO data) {
        return data.email() == null || data.password() == null || data.email().isBlank() || data.password().isBlank();
    }

    private boolean checkParamsIsNotNull(String email, String refreshToken) {
        return email == null || refreshToken == null || email.isBlank() || refreshToken.isBlank();
    }

}
