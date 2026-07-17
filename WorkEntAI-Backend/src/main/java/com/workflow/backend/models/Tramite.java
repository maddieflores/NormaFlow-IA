package com.workflow.backend.models;

import com.workflow.backend.enums.EstadoTramite;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "tramites")
public class Tramite {

    @Id
    private String id;

    // Referencia a la política que define el flujo
    @Indexed
    private String politicaId;

    // Nombre de la política (desnormalizado para mostrar en frontend sin join)
    private String nombrePolitica;

    // Cliente que inició el trámite
    @Indexed
    private String clienteId;

    // Nombre del cliente (desnormalizado)
    private String nombreCliente;

    // Nodo donde se encuentra actualmente el trámite
    private String nodoActualId;

    // Nombre del nodo actual (desnormalizado para el frontend)
    private String nombreNodoActual;

    // Departamento responsable del nodo actual
    private String departamentoActual;

    // Estado del trámite con semáforo visual
    @Builder.Default
    private EstadoTramite estado = EstadoTramite.NUEVO;

    // Descripción o motivo del trámite ingresado por el cliente
    private String descripcion;

    // Número de referencia legible (ej: TRM-2026-0001)
    @Indexed(unique = true)
    private String numeroReferencia;

    // Prioridad: BAJA, MEDIA, ALTA
    @Builder.Default
    private String prioridad = "MEDIA";

    // Historial de pasos completados
    @Builder.Default
    private List<HistorialTramite> historial = new ArrayList<>();

    // Datos acumulados de todos los formularios completados
    @Builder.Default
    private Map<String, Object> datosFormulario = new HashMap<>();

    // Archivos adjuntos subidos a S3
    @Builder.Default
    private List<ArchivoAdjunto> archivos = new ArrayList<>();

    // Observación final al completar o rechazar
    private String observacionFinal;

    /**
     * Contador de ramas paralelas pendientes por sincronizar en cada nodo JOIN (CU-13).
     * Clave: ID del nodo JOIN destino.
     * Valor: número de ramas que aún no han completado su ejecución.
     *
     * Cuando el valor llega a 0, el motor avanza el trámite más allá del JOIN.
     * Este mapa es inicializado por el motor al procesar un nodo PARALLEL (fork).
     */
    @Builder.Default
    private Map<String, Integer> ramasParalelasActivas = new HashMap<>();

    // Fechas de ciclo de vida
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private LocalDateTime fechaUltimaActualizacion;

    // Tiempo total en minutos (calculado al finalizar)
    private Long duracionMinutos;
}