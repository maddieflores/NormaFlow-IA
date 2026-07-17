package com.workflow.backend.controllers;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.models.Tarea;
import com.workflow.backend.services.NotificacionService;
import com.workflow.backend.services.TareaService;
import com.workflow.backend.services.WorkflowEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final TareaService tareaService;
    private final WorkflowEngineService workflowEngineService;
    private final NotificacionService notificacionService;

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<Tarea>> getByFuncionario(@PathVariable String funcionarioId) {
        return ResponseEntity.ok(tareaService.getByFuncionario(funcionarioId));
    }

    @GetMapping("/departamento/{dept}")
    public ResponseEntity<List<Tarea>> getByDepartamento(@PathVariable String dept) {
        return ResponseEntity.ok(tareaService.getByDepartamento(dept));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tarea> getById(@PathVariable String id) {
        return ResponseEntity.ok(tareaService.getById(id));
    }

    @PutMapping("/{id}/completar")
    public ResponseEntity<Void> completar(
            @PathVariable String id,
            @RequestBody Map<String, Object> formularioDatos) {
        workflowEngineService.completarTarea(id, formularioDatos);
        
        // Notify department about task update
        Tarea tarea = tareaService.getById(id);
        if (tarea.getDepartamento() != null) {
            notificacionService.notificarDepartamento(
                tarea.getDepartamento(),
                "Tarea actualizada: " + tarea.getNombreNodo(),
                "TAREA_ACTUALIZADA",
                tarea.getId(),
                "TAREA"
            );
        }
        
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Tarea> actualizarEstado(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        EstadoTarea nuevoEstado = EstadoTarea.valueOf(body.get("estado"));
        Tarea tarea = tareaService.actualizarEstado(id, nuevoEstado);
        if (tarea.getDepartamento() != null) {
            notificacionService.notificarDepartamento(
                tarea.getDepartamento(),
                "Estado de tarea actualizado: " + tarea.getNombreNodo(),
                "TAREA_ESTADO_ACTUALIZADO",
                tarea.getId(),
                "TAREA"
            );
        }
        return ResponseEntity.ok(tarea);
    }
}