package com.workflow.backend.services;

import com.workflow.backend.dto.CrearUsuarioRequest;
import com.workflow.backend.dto.UsuarioResponse;
import com.workflow.backend.enums.RolUsuario;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de gestión de usuarios (CU-03).
 *
 * SRP: única responsabilidad — operaciones CRUD sobre usuarios y asignación de roles/departamentos.
 * Utiliza DTOs de entrada/salida para no exponer el modelo de dominio directamente al controlador.
 * No contiene lógica de workflow ni de notificaciones.
 */
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Consultas ────────────────────────────────────────────────────────────

    public List<UsuarioResponse> getAll() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UsuarioResponse getByIdDto(String id) {
        return toResponse(getById(id));
    }

    /** Usado internamente por otros servicios que necesitan el modelo de dominio */
    public Usuario getById(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    /**
     * Retorna todos los funcionarios de un departamento específico.
     * Busca tanto en el campo legacy {@code departamento} como en la lista {@code departamentos}.
     */
    public List<UsuarioResponse> getByDepartamento(String departamento) {
        List<Usuario> porListaDepts = usuarioRepository.findByDepartamentosContaining(departamento);
        List<Usuario> porCampoLegacy = usuarioRepository.findByDepartamento(departamento);

        // Unir sin duplicados (un usuario puede aparecer en ambas listas)
        List<Usuario> combinados = new ArrayList<>(porListaDepts);
        porCampoLegacy.forEach(u -> {
            if (combinados.stream().noneMatch(c -> c.getId().equals(u.getId()))) {
                combinados.add(u);
            }
        });
        return combinados.stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ── Mutaciones ───────────────────────────────────────────────────────────

    /**
     * Crea un nuevo usuario (CU-03).
     * Hashea la contraseña, sincroniza el campo legacy departamento con el primero de la lista.
     */
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe un usuario registrado con el email: " + request.getEmail());
        }

        RolUsuario rol;
        try {
            rol = RolUsuario.valueOf(request.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Rol inválido: " + request.getRol() + ". Valores permitidos: ADMIN, FUNCIONARIO, CLIENTE");
        }

        List<String> departamentos = request.getDepartamentos() != null
                ? request.getDepartamentos() : new ArrayList<>();

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(rol);
        usuario.setDepartamentos(departamentos);
        // Sincronizar campo legacy con el primer departamento de la lista (retrocompatibilidad)
        usuario.setDepartamento(departamentos.isEmpty() ? null : departamentos.get(0));
        usuario.setActivo(true);

        return toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Actualiza datos básicos del usuario: nombre, departamentos y estado activo (CU-03).
     * No permite cambiar email (es el identificador único) ni contraseña por este endpoint.
     */
    public UsuarioResponse actualizar(String id, CrearUsuarioRequest request) {
        Usuario existente = getById(id);

        existente.setNombre(request.getNombre());

        List<String> departamentos = request.getDepartamentos() != null
                ? request.getDepartamentos() : new ArrayList<>();
        existente.setDepartamentos(departamentos);
        // Sincronizar campo legacy
        existente.setDepartamento(departamentos.isEmpty() ? null : departamentos.get(0));

        return toResponse(usuarioRepository.save(existente));
    }

    /** Desactivación lógica del usuario (CU-03 — no elimina datos históricos) */
    public void desactivar(String id) {
        Usuario usuario = getById(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    /** Eliminación física — usar solo en desarrollo o limpieza administrativa */
    public void eliminar(String id) {
        getById(id); // Verifica que exista
        usuarioRepository.deleteById(id);
    }

    // ── Mapeo DTO ────────────────────────────────────────────────────────────

    private UsuarioResponse toResponse(Usuario usuario) {
        UsuarioResponse dto = new UsuarioResponse();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setRol(usuario.getRol() != null ? usuario.getRol().name() : null);
        dto.setDepartamento(usuario.getDepartamento());
        dto.setDepartamentos(usuario.getDepartamentos() != null
                ? usuario.getDepartamentos() : new ArrayList<>());
        dto.setActivo(usuario.isActivo());
        return dto;
    }
}