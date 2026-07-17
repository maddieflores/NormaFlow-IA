package com.workflow.backend;

import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.models.Departamento;
import com.workflow.backend.models.Usuario;
import com.workflow.backend.repositories.DepartamentoRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.services.DepartamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DepartamentoService.delete()
 * Validates: Requirements 3.1, 3.4
 */
@ExtendWith(MockitoExtension.class)
class DepartamentoServiceTest {

    @Mock
    private DepartamentoRepository departamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DepartamentoService departamentoService;

    @Test
    void delete_throwsBusinessException_whenActiveUsersAssigned() {
        // Arrange
        String deptId = "dept-1";
        Departamento departamento = Departamento.builder()
                .id(deptId)
                .nombre("Recursos Humanos")
                .build();

        Usuario usuario = new Usuario();
        usuario.setId("user-1");
        usuario.setNombre("Juan");
        usuario.setDepartamento("Recursos Humanos");

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(departamento));
        when(usuarioRepository.findByDepartamento("Recursos Humanos")).thenReturn(List.of(usuario));

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class,
                () -> departamentoService.delete(deptId));

        assertTrue(ex.getMessage().contains("usuarios asignados"));
        verify(departamentoRepository, never()).deleteById(any());
    }

    @Test
    void delete_callsDeleteById_whenNoUsersAssigned() {
        // Arrange
        String deptId = "dept-2";
        Departamento departamento = Departamento.builder()
                .id(deptId)
                .nombre("Tecnología")
                .build();

        when(departamentoRepository.findById(deptId)).thenReturn(Optional.of(departamento));
        when(usuarioRepository.findByDepartamento("Tecnología")).thenReturn(Collections.emptyList());

        // Act
        departamentoService.delete(deptId);

        // Assert
        verify(departamentoRepository).deleteById(deptId);
    }
}
