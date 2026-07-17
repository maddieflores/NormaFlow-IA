package com.workflow.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String nombre;
    private String email;
    private String password;
    private String rol;
    private String departamento;
}