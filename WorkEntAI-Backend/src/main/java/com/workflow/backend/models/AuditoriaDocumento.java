package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

/**
 * Registro inmutable de auditoría de accesos a documentos (Ciclo 2 — CU-27).
 *
 * Cada acción realizada sobre un DocumentoTramite genera una entrada
 * en esta colección. Los registros NUNCA se eliminan (inmutabilidad de auditoría).
 *
 * Principio SRP: solo registra eventos, no contiene lógica de negocio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "auditoria_documentos")
public class AuditoriaDocumento {

    @Id
    private String id;

    /** ID del documento al que se accedió */
    @Indexed
    private String documentoId;

    /** ID del trámite (para facilitar consultas por trámite) */
    @Indexed
    private String tramiteId;

    /**
     * Tipo de acción ejecutada:
     * SUBIR | VER_URL | DESCARGAR | ELIMINAR | CAMBIAR_PERMISOS | SUBIR_VERSION
     */
    private String accion;

    /** ID del usuario que ejecutó la acción */
    @Indexed
    private String usuarioId;

    /** Nombre del usuario (desnormalizado) */
    private String nombreUsuario;

    /** Rol del usuario al momento de la acción */
    private String rolUsuario;

    /** Resultado de la operación: OK | DENEGADO | ERROR */
    private String resultado;

    /** Mensaje descriptivo del resultado (útil para errores o denegaciones) */
    private String detalle;

    /** Dirección IP del cliente (para seguridad y trazabilidad) */
    private String ipOrigen;

    /** Timestamp exacto del evento — indexado para consultas por rango de fechas */
    @Indexed
    private LocalDateTime timestamp;
}
