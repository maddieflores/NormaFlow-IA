package com.workflow.backend.models;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "departamentos")
public class Departamento {

    @Id
    private String id;

    @Indexed(unique = true)
    private String nombre;

    private String descripcion;

    private String responsableId;

    private LocalDateTime fechaCreacion;
}
