package com.workflow.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de entrada para crear un usuario (CU-03).
 *
 * SRP: clase de datos pura para transferencia — sin lógica de negocio.
 * Separar el modelo de dominio {@link com.workflow.backend.models.Usuario}
 * del objeto de entrada del controlador evita exponer campos internos
 * (id, fechaCreacion, activo) y permite validaciones independientes.
 */
@Data
public class CrearUsuarioRequest {

    @NotBlank(message = "El nombre es requerido")
    private String nombre;

    @NotBlank(message = "El email es requerido")
    @Email(message = "El email debe tener formato válido")
    private String email;

    @NotBlank(message = "La contraseña es requerida")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    /** Rol del usuario: ADMIN, FUNCIONARIO o CLIENTE */
    @NotBlank(message = "El rol es requerido")
    private String rol;

    /**
     * Lista de departamentos a los que pertenece el funcionario.
     * Solo aplica cuando {@code rol} es FUNCIONARIO.
     * Si se envía vacío o nulo para un FUNCIONARIO, no se asigna departamento.
     */
    private List<String> departamentos = new ArrayList<>();
}
