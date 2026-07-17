package com.workflow.backend.services;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Contrato de almacenamiento de archivos (DIP — Principio de Inversión de Dependencias).
 *
 * El resto del sistema depende de esta abstracción, no de una implementación concreta.
 * Esto permite intercambiar la implementación sin modificar los controladores ni el motor:
 *
 *  - Ciclo 1: {@link LocalArchivoStorage} — sistema de archivos local
 *  - Ciclo 2: {@code S3ArchivoStorage}   — Amazon S3 (/{policyId}/{tramiteId}/{nodeId}/{filename})
 *
 * Abierto para extensión, cerrado para modificación (OCP).
 */
public interface ArchivoStorage {

    /**
     * Persiste el archivo y retorna el nombre único asignado (UUID + extensión).
     *
     * @param archivo   Archivo recibido vía multipart
     * @param tramiteId ID del trámite al que pertenece (contexto de organización)
     * @return Nombre único del archivo guardado
     * @throws IOException si ocurre un error de escritura
     */
    String guardar(MultipartFile archivo, String tramiteId) throws IOException;

    /**
     * Carga un archivo para su lectura/descarga.
     *
     * @param nombreArchivo Nombre del archivo tal como fue retornado por {@link #guardar}
     * @return Recurso Spring para servir al cliente
     * @throws MalformedURLException si la ruta no es válida
     */
    Resource cargar(String nombreArchivo) throws MalformedURLException;

    /**
     * Genera la URL de acceso público para un archivo guardado.
     *
     * @param nombreArchivo Nombre único del archivo
     * @return URL relativa o absoluta según la implementación
     */
    String generarUrl(String nombreArchivo);
}
