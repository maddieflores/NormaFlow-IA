package com.workflow.backend.controllers;

import com.workflow.backend.services.ArchivoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Controlador de gestión de archivos adjuntos (CU-12 — Opción B: multipart).
 *
 * SRP: solo orquesta peticiones HTTP de archivos — delega almacenamiento a {@link ArchivoStorage}.
 * DIP: depende de la interfaz {@link ArchivoStorage}, no de {@code LocalArchivoStorage}.
 *      En Ciclo 2, sustituir la implementación por S3 sin tocar este controlador.
 */
@RestController
@RequestMapping("/api/archivos")
@RequiredArgsConstructor
public class ArchivoController {

    /** DIP: inyección por interfaz — permite intercambiar implementaciones (local ↔ S3) */
    private final ArchivoStorage archivoStorage;

    /**
     * POST /api/archivos/subir
     * Recibe un archivo vía multipart y retorna su nombre único y URL de acceso.
     *
     * Usado por la app móvil y el frontend al adjuntar documentos en tareas (CU-12).
     */
    @PostMapping(value = "/subir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subir(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "tramiteId", required = false, defaultValue = "") String tramiteId) {
        try {
            String nombre = archivoStorage.guardar(archivo, tramiteId);
            String url    = archivoStorage.generarUrl(nombre);
            return ResponseEntity.ok(Map.of(
                    "nombre",          nombre,
                    "url",             url,
                    "nombreOriginal",  archivo.getOriginalFilename() != null
                                         ? archivo.getOriginalFilename() : nombre,
                    "tamanioBytes",    String.valueOf(archivo.getSize())
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al guardar el archivo: " + e.getMessage()));
        }
    }

    /**
     * GET /api/archivos/{nombre}
     * Sirve o descarga un archivo por su nombre único asignado al subir.
     */
    @GetMapping("/{nombre}")
    public ResponseEntity<Resource> descargar(@PathVariable String nombre) {
        try {
            Resource recurso = archivoStorage.cargar(nombre);
            // Intentar detectar MIME; por defecto octet-stream (descarga segura)
            String contentType = "application/octet-stream";
            try {
                String detected = recurso.getURL().openConnection().getContentType();
                if (detected != null && !detected.equals("content/unknown")) {
                    contentType = detected;
                }
            } catch (Exception ignored) {}

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + recurso.getFilename() + "\"")
                    .body(recurso);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
