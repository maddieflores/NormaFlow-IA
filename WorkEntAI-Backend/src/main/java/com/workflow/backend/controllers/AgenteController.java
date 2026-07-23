package com.workflow.backend.controllers;

import com.workflow.backend.models.AgenteSession;
import com.workflow.backend.services.AgenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.UsuarioRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller del Agente Inteligente de Atención al Cliente (CU-22/23).
 *
 * Endpoints:
 * POST /api/agente/sesion — iniciar/retomar sesión de diálogo
 * POST /api/agente/sesion/{id}/mensaje — enviar mensaje y obtener respuesta
 * GET /api/agente/sesion/{id} — obtener estado actual de la sesión
 * DELETE /api/agente/sesion/{id} — cerrar sesión manualmente
 *
 * Principio SRP: solo orquesta HTTP; la lógica está en AgenteService.
 */
@RestController
@RequestMapping("/api/agente")
@RequiredArgsConstructor
public class AgenteController {

    private final AgenteService agenteService;
    private final UsuarioRepository usuarioRepository;

    /**
     * Inicia una nueva sesión de diálogo con el agente IA,
     * o retoma la sesión activa existente para el cliente.
     */
    @PostMapping("/sesion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<AgenteSession> iniciarSesion(Authentication auth) {
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new com.workflow.backend.exception.ResourceNotFoundException("Cliente", email));
        return ResponseEntity.ok(agenteService.iniciarSesion(usuario.getId()));
    }

    /**
     * Envía un mensaje del cliente al agente y recibe la respuesta.
     * El agente actualiza la fase del diálogo automáticamente.
     */
    @PostMapping("/sesion/{sessionId}/mensaje")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<AgenteSession> enviarMensaje(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String mensaje = body.getOrDefault("mensaje", "");
        return ResponseEntity.ok(agenteService.procesarMensaje(sessionId, mensaje));
    }

    /**
     * Obtiene el estado actual de una sesión (fase, mensajes, datos recopilados).
     * Útil para recargar la UI tras una reconexión.
     */
    @GetMapping("/sesion/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AgenteSession> obtenerSesion(@PathVariable String sessionId) {
        return ResponseEntity.ok(agenteService.obtenerSesion(sessionId));
    }

    /**
     * Cierra la sesión manualmente (el cliente abandona el diálogo).
     */
    @DeleteMapping("/sesion/{sessionId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> cerrarSesion(@PathVariable String sessionId) {
        agenteService.cerrarSesion(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Inicia una nueva sesión de diálogo para un usuario no autenticado (Demo).
     */
    @PostMapping("/demo/sesion")
    public ResponseEntity<AgenteSession> iniciarSesionDemo() {
        return ResponseEntity.ok(agenteService.iniciarSesionDemo());
    }

    /**
     * Envía un mensaje del cliente al agente en el contexto de demo.
     */
    @PostMapping("/demo/sesion/{sessionId}/mensaje")
    public ResponseEntity<AgenteSession> enviarMensajeDemo(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        String mensaje = body.getOrDefault("mensaje", "");
        return ResponseEntity.ok(agenteService.procesarMensaje(sessionId, mensaje));
    }

    /**
     * Cierra la sesión de demo manualmente.
     */
    @DeleteMapping("/demo/sesion/{sessionId}")
    public ResponseEntity<Void> cerrarSesionDemo(@PathVariable String sessionId) {
        agenteService.cerrarSesion(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reclama una sesión demo para un cliente autenticado.
     */
    @PostMapping("/sesion/reclamar/{sessionId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<AgenteSession> reclamarSesion(
            @PathVariable String sessionId,
            Authentication auth) {
        String email = auth.getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new com.workflow.backend.exception.ResourceNotFoundException("Cliente", email));
        return ResponseEntity.ok(agenteService.reclamarSesion(sessionId, usuario.getId()));
    }
}
