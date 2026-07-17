package com.workflow.backend.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.UUID;

/**
 * Implementación de {@link ArchivoStorage} usando Amazon S3 (Ciclo 2 — CU-24).
 *
 * Principios SOLID aplicados:
 * - SRP: única responsabilidad de subir/descargar archivos a S3.
 * - OCP: se puede decorar o reemplazar sin modificar ArchivoController.
 * - DIP: ArchivoController depende de la interfaz, no de esta clase.
 * - LSP: sustituye a LocalArchivoStorage sin cambios en los clientes.
 *
 * Estructura de keys en S3:
 * tramites/{tramiteId}/{uuid}-{nombreOriginal}
 */
@Slf4j
@Primary // Reemplaza LocalArchivoStorage como implementación activa
@Service
@RequiredArgsConstructor
public class S3ArchivoStorage implements ArchivoStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.presigned-url-expiration-minutes:60}")
    private int presignedExpirationMinutes;

    // ── Subir archivo ────────────────────────────────────────────────────────

    /**
     * Sube el archivo al bucket S3 bajo la ruta
     * tramites/{tramiteId}/{uuid}-{nombre}.
     *
     * @param archivo   Archivo recibido vía multipart
     * @param tramiteId ID del trámite (usado como prefijo de carpeta en S3)
     * @return S3 key del objeto guardado (se usa como identificador en MongoDB)
     */
    @Override
    public String guardar(MultipartFile archivo, String tramiteId) throws IOException {
        validarArchivo(archivo);

        String key = "tramites/" + tramiteId + "/" + UUID.randomUUID() + "-" +
                sanitizarNombre(archivo.getOriginalFilename());

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(archivo.getContentType())
                .contentLength(archivo.getSize())
                // Los objetos son privados: solo el backend puede acceder vía URL pre-firmada
                .serverSideEncryption(ServerSideEncryption.AES256)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(archivo.getBytes()));
        log.info("📁 Archivo subido a S3: {}", key);
        return key;
    }

    // ── Descargar archivo ────────────────────────────────────────────────────

    /**
     * Descarga el objeto de S3 como Resource de Spring.
     * Nota: para uso interno; en producción prefiere {@link #generarUrl}
     * (pre-signed URL).
     */
    @Override
    public Resource cargar(String key) throws MalformedURLException {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return new InputStreamResource(s3Client.getObject(request));
        } catch (NoSuchKeyException e) {
            throw new com.workflow.backend.exception.ResourceNotFoundException(
                    "Archivo no encontrado en S3: " + key);
        }
    }

    // ── URL pre-firmada ──────────────────────────────────────────────────────

    /**
     * Genera una URL pre-firmada temporal para descarga directa desde S3.
     * El frontend usa esta URL para descargar sin pasar por el backend.
     * Expiración: configurable via aws.s3.presigned-url-expiration-minutes (default
     * 60 min).
     */
    @Override
    public String generarUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedExpirationMinutes))
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.debug("🔗 URL pre-firmada generada para key={}, expira en {}min", key, presignedExpirationMinutes);
        return url;
    }

    /**
     * Elimina el objeto de S3 (soft-delete lógico se maneja en DocumentoService).
     */
    public void eliminar(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("🗑️  Archivo eliminado de S3: {}", key);
        } catch (S3Exception e) {
            log.warn("⚠️  Error al eliminar de S3 key={}: {}", key, e.getMessage());
        }
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private void validarArchivo(MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        long maxSize = 10L * 1024 * 1024; // 10 MB
        if (archivo.getSize() > maxSize) {
            throw new IllegalArgumentException("El archivo supera el límite de 10 MB");
        }
        // Validación de tipo MIME (whitelist)
        String contentType = archivo.getContentType();
        if (contentType == null || !esTipoPermitido(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido: " + contentType +
                            ". Tipos aceptados: PDF, imágenes (JPEG, PNG, WEBP), Word, Excel.");
        }
    }

    private boolean esTipoPermitido(String contentType) {
        return contentType.equals("application/pdf")
                || contentType.startsWith("image/")
                || contentType.startsWith("video/")
                || contentType.startsWith("audio/")
                || contentType.equals("application/msword")
                || contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || contentType.equals("application/vnd.ms-excel")
                || contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private String sanitizarNombre(String nombre) {
        if (nombre == null)
            return "archivo";
        // Elimina caracteres peligrosos para S3 keys
        return nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
