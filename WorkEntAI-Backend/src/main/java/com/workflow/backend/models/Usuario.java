package com.workflow.backend.models;

import com.workflow.backend.enums.RolUsuario;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;

    private String nombre;

    @Indexed(unique = true)
    private String email;

    private String password;

    private RolUsuario rol;

    // Departamento principal (campo legacy, mantenido por compatibilidad con el frontend actual)
    // Refleja el primer elemento de la lista `departamentos`
    private String departamento;

    // Lista de departamentos a los que pertenece el funcionario (CU-03 — multi-departamento)
    // Permite que un funcionario reciba tareas de más de un departamento
    private List<String> departamentos = new ArrayList<>();

    private boolean activo = true;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // Token FCM para notificaciones push móvil
    private String fcmToken;
}