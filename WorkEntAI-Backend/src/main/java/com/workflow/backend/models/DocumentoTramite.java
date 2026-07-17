package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un documento adjunto a un trámite (Ciclo 2 — CU-24 al CU-27).
 *
 * Cada documento se almacena en Amazon S3; este modelo guarda los metadatos
 * y los permisos de acceso (RBAC) en MongoDB.
 *
 * Estructura S3:  tramites/{tramiteId}/{uuid}-{nombre}
 *
 * Principio SRP: solo modela datos del documento, la lógica está en DocumentoService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "documentos_tramite")
public class DocumentoTramite {

    @Id
    private String id;

    /** ID del trámite al que pertenece este documento */
    @Indexed
    private String tramiteId;

    /** ID del nodo del workflow en cuyo contexto se subió (puede ser null si es adjunto global) */
    private String nodoId;

    /** Nombre original del archivo tal como lo subió el usuario */
    private String nombre;

    /** Descripción opcional del documento (ej: "Cédula de identidad escaneada") */
    private String descripcion;

    /** Tipo MIME del archivo (ej: application/pdf, image/jpeg) */
    private String tipoMime;

    /** Tamaño del archivo en bytes */
    private Long tamanoBytes;

    /** Versión del documento — incrementa cada vez que se sube una nueva versión */
    @Builder.Default
    private Integer version = 1;

    /**
     * Key del objeto en S3 (identificador único en el bucket).
     * Formato: tramites/{tramiteId}/{uuid}-{nombre}
     * Esta key se usa para generar URLs pre-firmadas.
     */
    private String s3Key;

    /** ID del usuario que subió el documento */
    private String subidoPorId;

    /** Nombre del usuario que subió (desnormalizado para facilitar la UI) */
    private String subidoPorNombre;

    /** Soft delete: false = visible; true = eliminado lógicamente */
    @Builder.Default
    private boolean activo = true;

    /**
     * Permisos de acceso por rol (RBAC — CU-26).
     * Define qué roles pueden VER, DESCARGAR o ELIMINAR este documento.
     */
    @Builder.Default
    private List<PermisoDocumento> permisos = new ArrayList<>();

    private LocalDateTime fechaSubida;
    private LocalDateTime fechaActualizacion;

    // ── Clase embebida de permisos RBAC ──────────────────────────────────────

    /**
     * Permiso de acceso a un documento por rol.
     * Principio LSP: es un value object inmutable, no una entidad.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermisoDocumento {
        /** Rol al que aplica (ADMIN, FUNCIONARIO, CLIENTE) */
        private String rol;
        /** Acciones permitidas: VER, DESCARGAR, ELIMINAR */
        @Builder.Default
        private List<String> acciones = new ArrayList<>();
    }
}
