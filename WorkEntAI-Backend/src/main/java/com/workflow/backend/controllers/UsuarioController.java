package com.workflow.backend.controllers;

import com.workflow.backend.dto.CrearUsuarioRequest;
import com.workflow.backend.dto.UsuarioResponse;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.services.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador de gestión de usuarios (CU-03).
 *
 * SRP: coordina solicitudes HTTP — delega toda la lógica de negocio a {@link UsuarioService}.
 * Usa DTOs de entrada/salida: no expone el modelo de dominio directamente.
 * Bean Validation con {@code @Valid} en los request bodies.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    /** GET /api/usuarios — Todos los usuarios (solo ADMIN) */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> getAll() {
        return ResponseEntity.ok(usuarioService.getAll());
    }

    /** GET /api/usuarios/{id} — Usuario por ID */
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(usuarioService.getByIdDto(id));
    }

    /**
     * GET /api/usuarios/departamento/{dept} — Funcionarios de un departamento (CU-03 multi-dept).
     * Busca tanto en el campo legacy como en la lista de departamentos.
     */
    @GetMapping("/departamento/{departamento}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<List<UsuarioResponse>> getByDepartamento(
            @PathVariable String departamento) {
        return ResponseEntity.ok(usuarioService.getByDepartamento(departamento));
    }

    /**
     * POST /api/usuarios — Crear usuario (solo ADMIN, CU-03).
     * Acepta {@link CrearUsuarioRequest} con validaciones Bean Validation.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
        UsuarioResponse creado = usuarioService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    /**
     * PUT /api/usuarios/{id} — Actualizar nombre/departamentos de un usuario (CU-03).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> actualizar(
            @PathVariable String id,
            @Valid @RequestBody CrearUsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    /**
     * DELETE /api/usuarios/{id} — Desactivación lógica (CU-03 — conserva histórico).
     * No elimina físicamente: solo marca activo=false.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> desactivar(@PathVariable String id) {
        usuarioService.desactivar(id);
        return ResponseEntity.ok(Map.of("mensaje", "Usuario desactivado correctamente"));
    }

    /**
     * POST /api/usuarios/{id}/fcm-token — Guarda token FCM del dispositivo móvil (CU-14).
     * Llamado automáticamente por la app Flutter al hacer login.
     */
    @PostMapping("/{id}/fcm-token")
    public ResponseEntity<Void> saveFcmToken(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String token = body.get("token");
        if (token != null && !token.isBlank()) {
            usuarioRepository.findById(id).ifPresent(u -> {
                u.setFcmToken(token);
                usuarioRepository.save(u);
            });
        }
        return ResponseEntity.ok().build();
    }
}