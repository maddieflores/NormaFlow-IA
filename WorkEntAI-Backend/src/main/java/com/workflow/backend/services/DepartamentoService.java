package com.workflow.backend.services;

import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.Departamento;
import com.workflow.backend.repositories.DepartamentoRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartamentoService {

    private final DepartamentoRepository departamentoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Departamento> getAll() {
        return departamentoRepository.findAll();
    }

    public Departamento getById(String id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento", id));
    }

    public Departamento create(Departamento departamento) {
        departamento.setFechaCreacion(LocalDateTime.now());
        return departamentoRepository.save(departamento);
    }

    public Departamento update(String id, Departamento updated) {
        Departamento existing = getById(id);
        existing.setNombre(updated.getNombre());
        existing.setDescripcion(updated.getDescripcion());
        existing.setResponsableId(updated.getResponsableId());
        return departamentoRepository.save(existing);
    }

    public void delete(String id) {
        Departamento departamento = getById(id);
        List<?> usuarios = usuarioRepository.findByDepartamento(departamento.getNombre());
        if (!usuarios.isEmpty()) {
            throw new BusinessException("No se puede eliminar el departamento: tiene usuarios asignados");
        }
        departamentoRepository.deleteById(id);
    }
}
