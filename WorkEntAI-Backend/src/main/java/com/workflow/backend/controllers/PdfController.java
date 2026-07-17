package com.workflow.backend.controllers;

import com.workflow.backend.models.Tramite;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.services.PdfGeneratorService;
import com.workflow.backend.services.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {

    private final TramiteService tramiteService;
    private final PdfGeneratorService pdfGeneratorService;
    private final UsuarioRepository usuarioRepository;

    /**
     * GET /api/pdf/tramite/{id}
     * Accessible by CLIENTE (own trámite only) or ADMIN.
     * Requirements: 15.2, 15.3, 15.4
     */
    @GetMapping("/tramite/{id}")
    public ResponseEntity<byte[]> descargarPdfTramite(
            @PathVariable String id,
            Authentication authentication) {

        Tramite tramite = tramiteService.getById(id);

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            // CLIENTE: verify ownership
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));

            if (!tramite.getClienteId().equals(usuario.getId())) {
                throw new AccessDeniedException("No tiene permiso para acceder a este trámite");
            }
        }

        byte[] pdfBytes = pdfGeneratorService.generarPdfTramite(tramite);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "tramite-" + tramite.getNumeroReferencia() + ".pdf"
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
