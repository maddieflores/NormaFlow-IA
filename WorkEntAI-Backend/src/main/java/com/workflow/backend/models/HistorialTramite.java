package com.workflow.backend.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialTramite {

    private String nodoId;
    private String nombreNodo;
    private String departamento;
    private String funcionarioId;
    private String nombreFuncionario;

    // Acción realizada: COMPLETADO, RECHAZADO, REDIRIGIDO, INICIADO
    private String accion;

    // Observación del funcionario al completar
    private String observacion;

    // Resultado de la decisión si el nodo era DECISION
    private String resultadoDecision; // APROBADO / RECHAZADO

    // Tiempo que tardó este paso (minutos)
    private Long duracionMinutos;

    private LocalDateTime fecha;
}