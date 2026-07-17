package com.workflow.backend;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.models.Tarea;
import com.workflow.backend.repositories.TareaRepository;
import com.workflow.backend.services.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 * Validates: Requirements 13.1
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TareaRepository tareaRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    // ── promediosPorNodo ──────────────────────────────────────────────────────

    @Test
    void promediosPorNodo_returnsCorrectAveragesPerNode() {
        Tarea t1 = Tarea.builder().nodoId("n1").duracionMinutos(10L).build();
        Tarea t2 = Tarea.builder().nodoId("n1").duracionMinutos(20L).build();
        Tarea t3 = Tarea.builder().nodoId("n2").duracionMinutos(30L).build();

        when(tareaRepository.findByPoliticaIdAndEstado("pol1", EstadoTarea.COMPLETADO))
                .thenReturn(List.of(t1, t2, t3));

        Map<String, Double> result = analyticsService.promediosPorNodo("pol1");

        assertEquals(2, result.size());
        assertEquals(15.0, result.get("n1"));
        assertEquals(30.0, result.get("n2"));
    }

    @Test
    void promediosPorNodo_returnsEmptyMapWhenNoCompletedTareas() {
        when(tareaRepository.findByPoliticaIdAndEstado("pol1", EstadoTarea.COMPLETADO))
                .thenReturn(Collections.emptyList());

        Map<String, Double> result = analyticsService.promediosPorNodo("pol1");

        assertTrue(result.isEmpty());
    }

    // ── promediosPorDepartamento ──────────────────────────────────────────────

    @Test
    void promediosPorDepartamento_returnsCorrectAveragesPerDepartment() {
        Tarea t1 = Tarea.builder().departamento("Legal").duracionMinutos(10L).build();
        Tarea t2 = Tarea.builder().departamento("Legal").duracionMinutos(20L).build();
        Tarea t3 = Tarea.builder().departamento("IT").duracionMinutos(60L).build();

        when(tareaRepository.findByEstado(EstadoTarea.COMPLETADO))
                .thenReturn(List.of(t1, t2, t3));

        Map<String, Double> result = analyticsService.promediosPorDepartamento();

        assertEquals(2, result.size());
        assertEquals(15.0, result.get("Legal"));
        assertEquals(60.0, result.get("IT"));
    }

    // ── eficienciaFuncionarios ────────────────────────────────────────────────

    @Test
    void eficienciaFuncionarios_sortedSlowestToFastest() {
        // funcionario "f1" average = 10, funcionario "f2" average = 50
        Tarea t1 = Tarea.builder().funcionarioId("f1").nombreFuncionario("Ana").duracionMinutos(10L).build();
        Tarea t2 = Tarea.builder().funcionarioId("f2").nombreFuncionario("Bob").duracionMinutos(50L).build();

        when(tareaRepository.findByDepartamentoAndEstado("IT", EstadoTarea.COMPLETADO))
                .thenReturn(List.of(t1, t2));

        List<Map<String, Object>> result = analyticsService.eficienciaFuncionarios("IT");

        assertEquals(2, result.size());
        // Slowest (highest average) first
        assertEquals("f2", result.get(0).get("funcionarioId"));
        assertEquals(50.0, result.get(0).get("promedioDuracion"));
        assertEquals("f1", result.get(1).get("funcionarioId"));
        assertEquals(10.0, result.get(1).get("promedioDuracion"));
    }
}
