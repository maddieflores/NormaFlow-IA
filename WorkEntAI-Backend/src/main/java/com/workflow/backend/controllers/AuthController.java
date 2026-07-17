package com.workflow.backend.controllers;

import com.workflow.backend.config.JwtUtil;

import com.workflow.backend.dto.AuthResponse;
import com.workflow.backend.dto.LoginRequest;
import com.workflow.backend.dto.RegisterRequest;
import com.workflow.backend.services.AuthService;
import com.workflow.backend.services.TokenBlacklist;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TokenBlacklist tokenBlacklistService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * CU-02 — Cerrar Sesión.
     * Invalida el token JWT actual en la blacklist para que no pueda ser reutilizado
     * incluso antes de su fecha de expiración natural.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                long expiry = jwtUtil.extractExpiration(token);
                tokenBlacklistService.invalidar(token, expiry);
            } catch (Exception ignored) {
                // Token ya inválido o malformado — igualmente cerrar sesión
            }
        }
        return ResponseEntity.ok(Map.of("mensaje", "Sesión cerrada correctamente"));
    }
}