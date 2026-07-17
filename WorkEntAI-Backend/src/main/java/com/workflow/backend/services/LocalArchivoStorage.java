package com.workflow.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Implementación Ciclo 1 de {@link ArchivoStorage}: almacenamiento en sistema de archivos local.
 *
 * SRP: responsabilidad única — persistir y recuperar archivos del disco.
 * OCP: en Ciclo 2 se crea {@code S3ArchivoStorage implements ArchivoStorage} sin
 *      tocar este código ni los controladores que dependen de la interfaz.
 *
 * Para migrar a S3: basta crear la nueva implementación y usar @Primary o @Profile("prod").
 */
@Slf4j
@Service
public class LocalArchivoStorage implements ArchivoStorage {

    private final Path directorioBase;
    private final String urlBase;

    public LocalArchivoStorage(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {

        this.directorioBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlBase = baseUrl;

        try {
            Files.createDirectories(this.directorioBase);
            log.info("Directorio de uploads inicializado en: {}", this.directorioBase);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de uploads: " + uploadDir, e);
        }
    }

    @Override
    public String guardar(MultipartFile archivo, String tramiteId) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo recibido está vacío o es nulo");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = extraerExtension(nombreOriginal);
        String nombreUnico = UUID.randomUUID() + extension;

        Path destino = directorioBase.resolve(nombreUnico).normalize();

        // Seguridad: evitar path traversal
        if (!destino.startsWith(directorioBase)) {
            throw new IllegalArgumentException("Ruta de destino inválida (path traversal detectado)");
        }

        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Archivo guardado: {} ({} bytes) | tramite: {}", nombreUnico, archivo.getSize(), tramiteId);

        return nombreUnico;
    }

    @Override
    public Resource cargar(String nombreArchivo) throws MalformedURLException {
        Path rutaArchivo = directorioBase.resolve(nombreArchivo).normalize();

        // Seguridad: evitar path traversal
        if (!rutaArchivo.startsWith(directorioBase)) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        Resource recurso = new UrlResource(rutaArchivo.toUri());
        if (!recurso.exists() || !recurso.isReadable()) {
            throw new RuntimeException("Archivo no encontrado o no legible: " + nombreArchivo);
        }
        return recurso;
    }

    @Override
    public String generarUrl(String nombreArchivo) {
        return urlBase + "/api/archivos/" + nombreArchivo;
    }

    // ── Utilidades privadas ──────────────────────────────────────────────────

    private String extraerExtension(String nombreOriginal) {
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            String ext = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            // Solo extensiones ASCII seguras (evitar null bytes)
            return ext.matches("\\.[a-zA-Z0-9]+") ? ext : "";
        }
        return "";
    }
}
