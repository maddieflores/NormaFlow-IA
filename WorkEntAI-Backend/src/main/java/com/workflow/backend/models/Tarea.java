package com.workflow.backend.models;

import com.workflow.backend.enums.EstadoTarea;
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
@Document(collection = "tareas")
public class Tarea {

    @Id
    private String id;

    @Indexed
    private String tramiteId;

    private String politicaId;

    private String nodoId;

    // Nombre del nodo (desnormalizado para el dashboard del funcionario)
    private String nombreNodo;

    // Departamento al que pertenece esta tarea
    private String departamento;

    @Indexed
    private String funcionarioId;

    // Nombre del funcionario (desnormalizado)
    private String nombreFuncionario;

    // Número de referencia del trámite (para mostrar en el dashboard)
    private String numeroReferenciaTramite;

    // Nombre de la política (para contexto en el dashboard)
    private String nombrePolitica;

    // Descripción de qué debe hacer el funcionario en este nodo
    private String instrucciones;

    // Campos del formulario definidos en el nodo (copiados al crear la tarea para que el funcionario sepa qué llenar)
    @Builder.Default
    private List<Map<String, Object>> camposFormulario = new ArrayList<>();

    @Builder.Default
    private EstadoTarea estado = EstadoTarea.PENDIENTE;

    // Datos del formulario completados por el funcionario
    @Builder.Default
    private Map<String, Object> formularioDatos = new HashMap<>();

    // Observación libre del funcionario
    private String observacion;

    // Prioridad heredada del trámite
    @Builder.Default
    private String prioridad = "MEDIA";

    private LocalDateTime fechaAsignacion;
    private LocalDateTime fechaCompletado;

    // Tiempo que tardó en completarse (minutos)
    private Long duracionMinutos;
}