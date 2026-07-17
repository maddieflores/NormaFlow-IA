package com.workflow.backend.controllers;

import com.workflow.backend.models.AuditoriaDocumento;
import com.workflow.backend.models.DocumentoTramite;
import com.workflow.backend.models.DocumentoTramite.PermisoDocumento;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.services.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST Controller para Gestión Documental (Ciclo 2 — CU-24 al CU-27).
 *
 * Endpoints:
 *   POST   /api/documentos/tramite/{tramiteId}        — subir documento
 *   GET    /api/documentos/tramite/{tramiteId}        — listar documentos
 *   GET    /api/documentos/{id}/url                   — URL pre-firmada descarga
 *   PUT    /api/documentos/{id}/permisos              — actualizar RBAC (ADMIN)
 *   DELETE /api/documentos/{id}                       — eliminar lógico
 *   GET    /api/documentos/{id}/auditoria             — historial de accesos (ADMIN)
 *   GET    /api/documentos/tramite/{tramiteId}/auditoria — auditoría completa trámite
 *
 * Principio SRP: solo orquesta HTTP; la lógica está en DocumentoService.
 */
@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    // ── POST /api/documentos/tramite/{tramiteId} ──────────────────────────────

    /**
     * Sube un documento asociado a un trámite.
     * Multipart: campo "archivo" (file) + params opcionales nodoId, descripcion.
     */
    @PostMapping("/tramite/{tramiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DocumentoTramite> subirDocumento(
            @PathVariable String tramiteId,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(required = false) String nodoId,
            @RequestParam(required = false) String descripcion,
            Authentication auth) throws IOException {

        Usuario u = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        String usuarioId = u != null ? u.getId() : auth.getName();
        String nombreUsuario = u != null ? u.getNombre() : obtenerNombreDeAuth(auth);

        DocumentoTramite doc = documentoService.subirDocumento(
                archivo, tramiteId, nodoId, descripcion, usuarioId, nombreUsuario);
        return ResponseEntity.ok(doc);
    }

    // ── GET /api/documentos/tramite/{tramiteId} ───────────────────────────────

    /**
     * Lista todos los documentos activos de un trámite.
     */
    @GetMapping("/tramite/{tramiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DocumentoTramite>> listarDocumentos(@PathVariable String tramiteId) {
        return ResponseEntity.ok(documentoService.listarDocumentos(tramiteId));
    }

    // ── GET /api/documentos/{id}/url ──────────────────────────────────────────

    /**
     * Genera y devuelve una URL pre-firmada temporal para descargar el documento directamente de S3.
     * La URL expira según aws.s3.presigned-url-expiration-minutes (default: 60 min).
     */
    @GetMapping("/{id}/url")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> obtenerUrlDescarga(
            @PathVariable String id, Authentication auth) {

        String rol = auth.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse("CLIENTE");

        Usuario u = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        String usuarioId = u != null ? u.getId() : auth.getName();
        String nombreUsuario = u != null ? u.getNombre() : obtenerNombreDeAuth(auth);

        String url = documentoService.obtenerUrlDescarga(
                id, usuarioId, nombreUsuario, rol);

        return ResponseEntity.ok(Map.of("url", url));
    }

    // ── PUT /api/documentos/{id}/permisos ─────────────────────────────────────

    /**
     * Actualiza la configuración RBAC de un documento (solo ADMIN).
     */
    @PutMapping("/{id}/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DocumentoTramite> actualizarPermisos(
            @PathVariable String id,
            @RequestBody List<PermisoDocumento> permisos,
            Authentication auth) {
        Usuario u = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        String usuarioId = u != null ? u.getId() : auth.getName();
        String nombreUsuario = u != null ? u.getNombre() : obtenerNombreDeAuth(auth);
        
        return ResponseEntity.ok(
                documentoService.actualizarPermisos(id, permisos, usuarioId, nombreUsuario));
    }

    // ── DELETE /api/documentos/{id} ───────────────────────────────────────────

    /**
     * Elimina lógicamente un documento (soft delete). Solo ADMIN y FUNCIONARIO.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable String id, Authentication auth) {
        Usuario u = usuarioRepository.findByEmail(auth.getName()).orElse(null);
        String usuarioId = u != null ? u.getId() : auth.getName();
        String nombreUsuario = u != null ? u.getNombre() : obtenerNombreDeAuth(auth);
        
        documentoService.eliminarDocumento(id, usuarioId, nombreUsuario);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/documentos/{id}/auditoria ────────────────────────────────────

    /**
     * Historial de accesos a un documento específico (solo ADMIN — CU-27).
     */
    @GetMapping("/{id}/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditoriaDocumento>> obtenerAuditoria(@PathVariable String id) {
        return ResponseEntity.ok(documentoService.obtenerAuditoria(id));
    }

    // ── GET /api/documentos/tramite/{tramiteId}/auditoria ─────────────────────

    /**
     * Todos los eventos de auditoría de los documentos de un trámite (solo ADMIN).
     */
    @GetMapping("/tramite/{tramiteId}/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditoriaDocumento>> obtenerAuditoriaTramite(
            @PathVariable String tramiteId) {
        return ResponseEntity.ok(documentoService.obtenerAuditoriaTramite(tramiteId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String obtenerNombreDeAuth(Authentication auth) {
        // El JWT puede tener el nombre en el principal o en los detalles
        return auth.getName();
    }
}
