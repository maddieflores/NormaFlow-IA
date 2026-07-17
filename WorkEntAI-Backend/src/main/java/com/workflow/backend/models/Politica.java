package com.workflow.backend.models;

import com.workflow.backend.enums.EstadoPolitica;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "politicas")
public class Politica {

    @Id
    private String id;

    private String nombre;

    // Descripción detallada del proceso que representa
    private String descripcion;

    // Categoría del negocio (ej: "Servicios Públicos", "Bancario", "Salud")
    private String categoria;

    // Organización o empresa a la que pertenece
    private String organizacion;

    // Tiempo estimado total del proceso en días
    private Integer tiempoEstimadoDias;

    // Etiquetas y palabras clave usadas por el Agente Inteligente (CU-22, RF-113)
    // El agente vectoriza estos términos para identificar la política adecuada para cada cliente
    @Builder.Default
    private List<String> etiquetas = new ArrayList<>();

    // Requisitos documentales iniciales que el cliente debe presentar al iniciar el trámite (CU-04, RF-013)
    // Cada requisito indica si es obligatorio u opcional
    @Builder.Default
    private List<RequisitoTramite> requisitosIniciales = new ArrayList<>();

    // Nodos del diagrama de actividades
    @Builder.Default
    private List<Nodo> nodos = new ArrayList<>();

    @Builder.Default
    private EstadoPolitica estado = EstadoPolitica.BORRADOR;

    // ID del admin que la creó
    private String creadoPorId;

    // Nombre del creador (desnormalizado)
    private String nombreCreadoPor;

    // Versión de la política (para control de cambios)
    @Builder.Default
    private Integer version = 1;

    // Cantidad de trámites activos bajo esta política
    @Builder.Default
    private Integer tramitesActivos = 0;

    // Cantidad total de trámites completados
    @Builder.Default
    private Integer tramitesCompletados = 0;

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private LocalDateTime fechaActivacion;
}