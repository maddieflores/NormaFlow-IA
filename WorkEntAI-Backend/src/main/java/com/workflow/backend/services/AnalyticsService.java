package com.workflow.backend.services;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.models.Tarea;
import com.workflow.backend.repositories.TareaRepository;
import com.workflow.backend.repositories.TramiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final TareaRepository tareaRepository;
    private final TramiteRepository tramiteRepository;

    /**
     * Returns average duracionMinutos per nodoId for COMPLETADO Tareas of a
     * specific policy.
     * Requirements: 13.1
     */
    public Map<String, Double> promediosPorNodo(String politicaId) {
        List<Tarea> tareas = tareaRepository.findByPoliticaIdAndEstado(politicaId, EstadoTarea.COMPLETADO);

        return tareas.stream()
                .filter(t -> t.getDuracionMinutos() != null)
                .collect(Collectors.groupingBy(
                        Tarea::getNodoId,
                        Collectors.averagingLong(Tarea::getDuracionMinutos)));
    }

    /**
     * Returns average duracionMinutos per departamento for all COMPLETADO Tareas.
     * Requirements: 13.3
     */
    public Map<String, Double> promediosPorDepartamento() {
        List<Tarea> tareas = tareaRepository.findByEstado(EstadoTarea.COMPLETADO);

        return tareas.stream()
                .filter(t -> t.getDuracionMinutos() != null && t.getDepartamento() != null)
                .collect(Collectors.groupingBy(
                        Tarea::getDepartamento,
                        Collectors.averagingLong(Tarea::getDuracionMinutos)));
    }

    /**
     * Returns per-funcionario average duracionMinutos within a department,
     * sorted slowest to fastest (highest average first).
     * Requirements: 13.4
     */
    public List<Map<String, Object>> eficienciaFuncionarios(String departamento) {
        List<Tarea> tareas = tareaRepository.findByDepartamentoAndEstado(departamento, EstadoTarea.COMPLETADO);

        Map<String, List<Tarea>> tareasPorFuncionario = tareas.stream()
                .filter(t -> t.getDuracionMinutos() != null && t.getFuncionarioId() != null)
                .collect(Collectors.groupingBy(Tarea::getFuncionarioId));

        return tareasPorFuncionario.entrySet().stream()
                .map(entry -> {
                    String funcionarioId = entry.getKey();
                    List<Tarea> tareasFunc = entry.getValue();

                    double promedio = tareasFunc.stream()
                            .mapToLong(Tarea::getDuracionMinutos)
                            .average()
                            .orElse(0.0);

                    String nombreFuncionario = tareasFunc.isEmpty() ? "" : tareasFunc.get(0).getNombreFuncionario();

                    Map<String, Object> result = new HashMap<>();
                    result.put("funcionarioId", funcionarioId);
                    result.put("nombreFuncionario", nombreFuncionario);
                    result.put("promedioDuracion", promedio);

                    return result;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("promedioDuracion"),
                        (Double) a.get("promedioDuracion")))
                .collect(Collectors.toList());
    }

    /**
     * Returns global KPI summary: total trámites, completion rate, avg duration,
     * active trámites.
     * Principio SRP: método dedicado exclusivamente a métricas globales del
     * sistema.
     */
    public Map<String, Object> kpisGlobales() {
        long totalTramites = tramiteRepository.count();
        long completados = tramiteRepository.findByEstado(EstadoTramite.COMPLETADO).size();
        long activos = tramiteRepository.findByEstado(EstadoTramite.EN_PROCESO).size();
        long rechazados = tramiteRepository.findByEstado(EstadoTramite.RECHAZADO).size();

        double tasaCompletado = totalTramites > 0 ? (completados * 100.0 / totalTramites) : 0;

        OptionalDouble avgDuracion = tramiteRepository.findByEstado(EstadoTramite.COMPLETADO).stream()
                .filter(t -> t.getDuracionMinutos() != null)
                .mapToLong(t -> t.getDuracionMinutos())
                .average();

        long totalTareas = tareaRepository.count();
        long tareasCompletadas = tareaRepository.findByEstado(EstadoTarea.COMPLETADO).size();
        long tareasPendientes = tareaRepository.findByEstado(EstadoTarea.PENDIENTE).size();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("totalTramites", totalTramites);
        kpis.put("tramitesCompletados", completados);
        kpis.put("tramitesActivos", activos);
        kpis.put("tramitesRechazados", rechazados);
        kpis.put("tasaCompletadoPct", Math.round(tasaCompletado * 10.0) / 10.0);
        kpis.put("duracionPromedioMinutos", avgDuracion.isPresent() ? Math.round(avgDuracion.getAsDouble()) : 0);
        kpis.put("totalTareas", totalTareas);
        kpis.put("tareasCompletadas", tareasCompletadas);
        kpis.put("tareasPendientes", tareasPendientes);
        return kpis;
    }

    /**
     * Alias de kpisGlobales() enriquecido con promedios por departamento.
     * Usado por ReporteController para pasar contexto de datos a la IA (CU-30).
     */
    public Map<String, Object> resumenGlobal() {
        Map<String, Object> resumen = new LinkedHashMap<>(kpisGlobales());
        resumen.put("promediosPorDepartamento", promediosPorDepartamento());
        return resumen;
    }

    public long getCountTareasCompletadasPorPolitica(String politicaId) {
        return tareaRepository.findByPoliticaIdAndEstado(politicaId, EstadoTarea.COMPLETADO).size();
    }

    public double getPromedioDuracionPorPolitica(String politicaId) {
        List<Tarea> tareas = tareaRepository.findByPoliticaIdAndEstado(politicaId, EstadoTarea.COMPLETADO);
        return tareas.stream()
                .filter(t -> t.getDuracionMinutos() != null)
                .mapToLong(Tarea::getDuracionMinutos)
                .average()
                .orElse(0.0);
    }
}
