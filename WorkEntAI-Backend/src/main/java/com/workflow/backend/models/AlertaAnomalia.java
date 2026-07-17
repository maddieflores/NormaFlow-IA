package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "alertas_anomalias")
public class AlertaAnomalia {
    @Id
    private String id;
    
    private String tramiteId;
    private String politicaId;
    private String nodoId;
    
    // Nivel: "BAJO", "MEDIO", "ALTO", "CRITICO"
    private String nivelGravedad;
    
    // Descripción de la anomalía detectada (ej: "Tiempo de permanencia excede el 200% del promedio histórico")
    private String descripcion;
    
    // Score devuelto por el LSTM
    private double score;
    
    @Builder.Default
    private boolean resuelta = false;
    
    @Builder.Default
    private LocalDateTime fechaDeteccion = LocalDateTime.now();
}
