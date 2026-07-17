package com.workflow.backend.services;

import com.workflow.backend.config.JwtUtil;
import com.workflow.backend.dto.AuthResponse;
import com.workflow.backend.dto.LoginRequest;
import com.workflow.backend.dto.RegisterRequest;
import com.workflow.backend.enums.RolUsuario;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BusinessException("Contraseña incorrecta");
        }

        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRol().name(), usuario.getDepartamento());
    }

    public AuthResponse register(RegisterRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(RolUsuario.valueOf(request.getRol().toUpperCase()));
        usuario.setDepartamento(request.getDepartamento());

        usuarioRepository.save(usuario);

        String token = jwtUtil.generateToken(usuario.getEmail(), usuario.getRol().name());
        return new AuthResponse(token, usuario.getId(), usuario.getEmail(), usuario.getNombre(), usuario.getRol().name(), usuario.getDepartamento());
    }
}