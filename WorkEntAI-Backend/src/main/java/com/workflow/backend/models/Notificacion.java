package com.workflow.backend.models;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;

    @Indexed
    private String usuarioId;

    private String mensaje;

    // Tipos: NUEVA_TAREA, TRAMITE_COMPLETADO, TRAMITE_RECHAZADO,
    //        CUELLO_BOTELLA, TAREA_VENCIDA, SISTEMA
    private String tipo;

    // Referencia al recurso relacionado (tramiteId o tareaId)
    private String referenciaId;

    // Tipo de referencia: TRAMITE, TAREA
    private String tipoReferencia;

    @Builder.Default
    private boolean leida = false;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaLeida;
}