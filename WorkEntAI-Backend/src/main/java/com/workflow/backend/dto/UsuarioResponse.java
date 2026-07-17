package com.workflow.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de salida que representa los datos públicos de un usuario (CU-03).
 *
 * SRP: expone solo los campos necesarios para el consumidor,
 * ocultando la contraseña hasheada y otros campos internos del modelo de dominio.
 */
@Data
public class UsuarioResponse {

    private String id;
    private String nombre;
    private String email;
    private String rol;

    /** Departamento principal (legacy, retrocompatibilidad con el frontend actual) */
    private String departamento;

    /** Lista completa de departamentos (CU-03 — multi-departamento) */
    private List<String> departamentos = new ArrayList<>();

    private boolean activo;
}
