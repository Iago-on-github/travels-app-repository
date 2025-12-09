package com.travel_system.backend_app.security;

import com.travel_system.backend_app.config.TokenConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenConfig tokenConfig;

    public JwtAuthenticationFilter(TokenConfig tokenConfig) {
        this.tokenConfig = tokenConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = tokenConfig.resolveToken(request);
        System.out.println("Token recebido: " + token);
        if (token != null) {
            try {
                System.out.println("Token válido? " + tokenConfig.validateToken(token));
                Authentication auth = tokenConfig.getAuthentication(token);
                System.out.println("Authorities: " + auth.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                System.out.println("Erro na autenticação JWT: " + e.getMessage());
            }
        } else {
            System.out.println("Nenhum token encontrado no request.");
        }
        filterChain.doFilter(request, response);
    }

}
