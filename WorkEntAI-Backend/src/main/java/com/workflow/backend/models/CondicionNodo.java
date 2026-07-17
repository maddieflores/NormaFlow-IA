package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Condición evaluable para un nodo de tipo DECISION (CU-05, Opción B).
 * Permite evaluar expresiones basadas en los datos del formulario completado.
 * Ejemplo: campo="monto", operador=">", valor="1000" → si monto > 1000 → ir a nodoSiId
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CondicionNodo {

    /** Nombre del campo del formulario a evaluar (ej: "monto", "aprobado", "resultado") */
    private String campo;

    /**
     * Operador de comparación.
     * Valores válidos: ==, !=, >, <, >=, <=, contains
     */
    private String operador;

    /** Valor de referencia contra el que se compara el campo (como String) */
    private String valor;

    /** ID del nodo destino si la condición evalúa a verdadero */
    private String nodoSiId;

    /** ID del nodo destino si la condición evalúa a falso */
    private String nodoNoId;
}
