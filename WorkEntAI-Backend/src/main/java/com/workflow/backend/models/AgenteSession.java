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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sesión de diálogo del Agente Inteligente (Ciclo 2 — CU-22/23).
 *
 * Persiste el estado de la conversación entre el cliente y el agente IA,
 * permitiendo retomar el diálogo si la sesión se interrumpe.
 *
 * Fases del diálogo:
 *   IDENTIFICACION → el agente identifica qué política aplica
 *   REQUISITOS     → el agente guía al cliente por los requisitos iniciales
 *   CONFIRMACION   → el cliente confirma y el sistema inicia el trámite
 *   COMPLETADA     → el trámite fue iniciado, sesión cerrada
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "agente_sesiones")
public class AgenteSession {

    @Id
    private String id;

    /** ID del cliente que está usando el agente */
    @Indexed
    private String clienteId;

    /** ID de la política identificada (null hasta que el agente la determina) */
    private String politicaId;

    /** Nombre de la política identificada (desnormalizado para la UI) */
    private String nombrePolitica;

    /**
     * Fase actual del diálogo:
     * IDENTIFICACION | REQUISITOS | CONFIRMACION | COMPLETADA | ABANDONADA
     */
    @Builder.Default
    private String fase = "IDENTIFICACION";

    /**
     * Historial de mensajes del diálogo.
     * rol: "USUARIO" | "AGENTE"
     */
    @Builder.Default
    private List<MensajeDialogo> mensajes = new ArrayList<>();

    /**
     * Datos recopilados durante el diálogo (mapa campo → valor).
     * Se usa para pre-llenar el formulario inicial del trámite.
     */
    @Builder.Default
    private Map<String, String> datosRecopilados = new HashMap<>();

    /**
     * Índice del requisito actual que se está validando (fase REQUISITOS).
     * Permite reanudar desde el punto correcto si la sesión se interrumpe.
     */
    @Builder.Default
    private Integer requisitoActualIndex = 0;

    /** ID del trámite creado al finalizar (fase COMPLETADA) */
    private String tramiteId;

    /** Si false, la sesión expiró o fue abandonada */
    @Builder.Default
    private boolean activa = true;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    // ── Clase embebida para mensajes ──────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MensajeDialogo {
        /** USUARIO | AGENTE */
        private String rol;
        private String contenido;
        private LocalDateTime timestamp;
    }
}
