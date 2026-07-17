package com.workflow.backend.controllers;

import com.workflow.backend.models.Tramite;
import com.workflow.backend.services.TramiteService;
import com.workflow.backend.repositories.TramiteRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tramites")
@RequiredArgsConstructor
public class TramiteController {

    private final TramiteService tramiteService;
    private final TramiteRepository tramiteRepository;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Tramite>> getAll() {
        return ResponseEntity.ok(tramiteService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tramite> getById(@PathVariable String id) {
        return ResponseEntity.ok(tramiteService.getById(id));
    }

    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<Tramite>> getByCliente(@PathVariable String clienteId) {
        return ResponseEntity.ok(tramiteService.getByCliente(clienteId));
    }

    @PostMapping("/iniciar")
    public ResponseEntity<Tramite> iniciar(
            @RequestParam String politicaId,
            @RequestParam String clienteId,
            @RequestParam(required = false, defaultValue = "") String descripcion,
            @RequestParam(required = false, defaultValue = "MEDIA") String prioridad) {
        return ResponseEntity.ok(tramiteService.iniciarTramite(politicaId, clienteId, descripcion, prioridad));
    }

    @PostMapping("/iniciar-por-email")
    public ResponseEntity<Tramite> iniciarPorEmail(
            @RequestParam String politicaId,
            @RequestParam String clienteEmail,
            @RequestParam(required = false, defaultValue = "") String descripcion) {
        String clienteId = usuarioRepository.findByEmail(clienteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente con email: " + clienteEmail))
                .getId();
        return ResponseEntity.ok(tramiteService.iniciarTramite(politicaId, clienteId, descripcion));
    }

    // Endpoint para que el cliente vea el progreso detallado de su trámite
    @GetMapping("/{id}/progreso")
    public ResponseEntity<Map<String, Object>> getProgreso(@PathVariable String id) {
        Tramite tramite = tramiteService.getById(id);
        return ResponseEntity.ok(Map.of(
                "numeroReferencia", tramite.getNumeroReferencia() != null ? tramite.getNumeroReferencia() : "",
                "estado", tramite.getEstado(),
                "nombrePolitica", tramite.getNombrePolitica() != null ? tramite.getNombrePolitica() : "",
                "nodoActual", tramite.getNombreNodoActual() != null ? tramite.getNombreNodoActual() : "",
                "departamentoActual", tramite.getDepartamentoActual() != null ? tramite.getDepartamentoActual() : "",
                "historial", tramite.getHistorial() != null ? tramite.getHistorial() : List.of(),
                "fechaInicio", tramite.getFechaInicio() != null ? tramite.getFechaInicio().toString() : "",
                "fechaFin", tramite.getFechaFin() != null ? tramite.getFechaFin().toString() : ""));
    }

    @PutMapping("/{id}/iniciar")
    public ResponseEntity<Tramite> iniciarPorAdmin(@PathVariable String id) {
        return ResponseEntity.ok(tramiteService.iniciarPorAdmin(id));
    }

    @GetMapping("/referencia/{ref}")
    public ResponseEntity<Tramite> getByReferencia(@PathVariable String ref) {
        Tramite tramite = tramiteRepository.findByNumeroReferencia(ref)
                .orElseThrow(() -> new ResourceNotFoundException("Trámite con referencia: " + ref));
        return ResponseEntity.ok(tramite);
    }
}