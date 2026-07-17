package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requisito documental que el cliente debe presentar al iniciar un trámite.
 * Definido por el Administrador en CU-04 al crear la Política de Negocio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequisitoTramite {

    /** Nombre del documento o requisito (ej: "Cédula de identidad") */
    private String nombre;

    /** Descripción adicional o instrucción para el cliente */
    private String descripcion;

    /** Si es true, el trámite no puede iniciar sin este documento */
    private boolean obligatorio;
}
