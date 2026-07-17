package com.workflow.backend.controllers;

import com.workflow.backend.models.ArchivoAdjunto;
import com.workflow.backend.models.Tramite;
import com.workflow.backend.services.ArchivoStorage;
import com.workflow.backend.services.TramiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/tramites")
@RequiredArgsConstructor
public class TramiteArchivoController {

    private final ArchivoStorage archivoStorage;
    private final TramiteService tramiteService;

    // Acepta PDF, imágenes, Word, Excel, Video y Audio
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "pdf", "jpg", "jpeg", "png", "docx", "doc", "xls", "xlsx", "mp4", "avi", "mov", "mp3", "wav"
    );

    @PostMapping(value = "/{tramiteId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ArchivoAdjunto> subirArchivo(
            @PathVariable String tramiteId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("nombreRequisito") String nombreRequisito) {

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo debe tener una extensión válida.");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de archivo no permitido: " + extension);
        }

        Tramite tramite = tramiteService.getById(tramiteId);

        try {
            // Utilizamos el S3ArchivoStorage inyectado
            String s3Key = archivoStorage.guardar(file, tramiteId);

            ArchivoAdjunto archivo = ArchivoAdjunto.builder()
                    .id(UUID.randomUUID().toString())
                    .nombreRequisito(nombreRequisito)
                    .nombreArchivoOriginal(originalFilename)
                    .s3Key(s3Key)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .fechaSubida(LocalDateTime.now())
                    .build();

            tramite.getArchivos().add(archivo);
            tramiteService.save(tramite);

            return ResponseEntity.ok(archivo);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir el archivo", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{tramiteId}/archivos/{archivoId}/descargar")
    public ResponseEntity<Map<String, String>> descargarArchivo(
            @PathVariable String tramiteId,
            @PathVariable String archivoId) {

        Tramite tramite = tramiteService.getById(tramiteId);

        ArchivoAdjunto archivo = tramite.getArchivos().stream()
                .filter(a -> a.getId().equals(archivoId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado en el trámite"));

        String presignedUrl = archivoStorage.generarUrl(archivo.getS3Key());
        return ResponseEntity.ok(Map.of("url", presignedUrl));
    }
}
