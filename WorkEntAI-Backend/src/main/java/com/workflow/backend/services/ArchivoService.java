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
 * Servicio de almacenamiento de archivos para adjuntos de trámites (CU-12, CU-16).
 *
 * Implementación Ciclo 1: almacenamiento en sistema de archivos local.
 * Ciclo 2: migrar a Amazon S3 bajo la estructura /{policyId}/{tramiteId}/{nodeId}/{filename}.
 */
@Slf4j
@Service
public class ArchivoService {

    private final Path directorioBase;

    public ArchivoService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.directorioBase = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.directorioBase);
            log.info("Directorio de uploads inicializado en: {}", this.directorioBase);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de uploads: " + uploadDir, e);
        }
    }

    /**
     * Guarda un archivo subido por el usuario y retorna el nombre único asignado.
     *
     * @param archivo   Archivo recibido via multipart
     * @param tramiteId ID del trámite (para organización, no usado en path Ciclo 1)
     * @return Nombre del archivo guardado (UUID + extensión original)
     */
    public String guardar(MultipartFile archivo, String tramiteId) throws IOException {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }

        // UUID garantiza nombres únicos sin colisiones
        String nombreUnico = UUID.randomUUID().toString() + extension;
        Path destino = this.directorioBase.resolve(nombreUnico);

        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        log.info("Archivo guardado: {} ({} bytes) para trámite {}", nombreUnico, archivo.getSize(), tramiteId);
        return nombreUnico;
    }

    /**
     * Carga un archivo para descarga/streaming.
     *
     * @param nombreArchivo Nombre del archivo (tal como fue retornado por guardar())
     * @return Recurso para servir al cliente
     */
    public Resource cargar(String nombreArchivo) throws MalformedURLException {
        Path rutaArchivo = directorioBase.resolve(nombreArchivo).normalize();
        Resource recurso = new UrlResource(rutaArchivo.toUri());
        if (!recurso.exists()) {
            throw new RuntimeException("Archivo no encontrado: " + nombreArchivo);
        }
        return recurso;
    }
}
