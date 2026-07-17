package com.workflow.backend.services;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.Tarea;
import com.workflow.backend.repositories.TareaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TareaService {

    private final TareaRepository tareaRepository;

    public List<Tarea> getByFuncionario(String funcionarioId) {
        return tareaRepository.findByFuncionarioId(funcionarioId);
    }

    public Tarea getById(String id) {
        return tareaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea", id));
    }

    public Tarea actualizarEstado(String tareaId, EstadoTarea nuevoEstado) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea", tareaId));
        tarea.setEstado(nuevoEstado);
        return tareaRepository.save(tarea);
    }

    public List<Tarea> getByDepartamento(String departamento) {
        return tareaRepository.findByDepartamento(departamento);
    }
}