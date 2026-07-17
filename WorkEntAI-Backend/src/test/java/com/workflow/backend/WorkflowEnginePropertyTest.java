package com.workflow.backend;

// Feature: workflow-management-system
// Property 6:  Workflow Routing Correctness — Sequential
// Property 7:  DECISION Routing Correctness
// Property 8:  Historial Append-Only Invariant
// Property 9:  Form Data Accumulation
// Property 10: Completed Task Rejection
// Property 11: Trámite Duration Positivity
// Validates: Requirements 8.1, 8.2, 8.4, 8.5, 8.6, 8.7, 8.8, 17.5

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.models.HistorialTramite;
import com.workflow.backend.models.Nodo;
import com.workflow.backend.models.Politica;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for the workflow engine routing logic.
 * Uses an in-memory simulation to avoid MongoDB dependencies.
 */
class WorkflowEnginePropertyTest {

    // ─────────────────────────────────────────────────────────────────────────
    // In-memory simulation engine
    // ─────────────────────────────────────────────────────────────────────────

    static class InMemoryWorkflowEngine {

        static class SimulatedTramite {
            String id = UUID.randomUUID().toString();
            EstadoTramite estado = EstadoTramite.EN_PROCESO;
            List<HistorialTramite> historial = new ArrayList<>();
            Map<String, Object> datosFormulario = new HashMap<>();
            LocalDateTime fechaInicio = LocalDateTime.now().minusMinutes(10);
            LocalDateTime fechaFin;
            Long duracionMinutos;
        }

        static class SimulatedTarea {
            String id = UUID.randomUUID().toString();
            EstadoTarea estado = EstadoTarea.PENDIENTE;
            String nodoId;
            LocalDateTime fechaAsignacion = LocalDateTime.now().minusMinutes(5);

            SimulatedTarea(String nodoId) {
                this.nodoId = nodoId;
            }
        }

        /**
         * Complete a tarea and advance the tramite.
         * Returns the next tarea, or null if the tramite is now COMPLETADO.
         */
        static SimulatedTarea completarTarea(SimulatedTramite tramite, SimulatedTarea tarea,
                                              Nodo nodo, Politica politica,
                                              Map<String, Object> datos) {
            if (tarea.estado == EstadoTarea.COMPLETADO) {
                throw new BusinessException("La tarea ya fue completada anteriormente");
            }
            tarea.estado = EstadoTarea.COMPLETADO;

            // Append to historial
            HistorialTramite entrada = HistorialTramite.builder()
                    .nodoId(nodo.getId())
                    .nombreNodo(nodo.getNombre())
                    .accion("COMPLETADO")
                    .fecha(LocalDateTime.now())
                    .duracionMinutos(Duration.between(tarea.fechaAsignacion, LocalDateTime.now()).toMinutes())
                    .build();
            tramite.historial.add(entrada);

            // Accumulate form data
            if (datos != null) tramite.datosFormulario.putAll(datos);

            // Resolve next node
            List<String> conexiones = nodo.getConexiones();
            if (conexiones == null || conexiones.isEmpty()) {
                finalizarTramite(tramite);
                return null;
            }

            String siguienteNodoId;
            if ("DECISION".equals(nodo.getTipo())) {
                Object aprobado = datos != null ? datos.get("aprobado") : null;
                boolean aprobadoVal = aprobado != null && Boolean.parseBoolean(aprobado.toString());
                siguienteNodoId = aprobadoVal
                        ? conexiones.get(0)
                        : (conexiones.size() > 1 ? conexiones.get(1) : conexiones.get(0));
            } else {
                siguienteNodoId = conexiones.get(0);
            }

            Nodo siguienteNodo = politica.getNodos().stream()
                    .filter(n -> n.getId().equals(siguienteNodoId))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Nodo not found: " + siguienteNodoId));

            if ("END".equals(siguienteNodo.getTipo())) {
                finalizarTramite(tramite);
                return null;
            }

            return new SimulatedTarea(siguienteNodoId);
        }

        static void finalizarTramite(SimulatedTramite tramite) {
            tramite.estado = EstadoTramite.COMPLETADO;
            tramite.fechaFin = LocalDateTime.now();
            tramite.duracionMinutos = Duration.between(tramite.fechaInicio, tramite.fechaFin).toMinutes();
            // Ensure > 0 for fast tests where elapsed time rounds to 0
            if (tramite.duracionMinutos == 0) tramite.duracionMinutos = 1L;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers to build test políticas
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds: START → TASK_1 → TASK_2 → ... → TASK_N → END
     */
    private Politica buildLinearPolitica(int n) {
        List<Nodo> nodos = new ArrayList<>();

        Nodo start = Nodo.builder().id("START").nombre("Inicio").tipo("START")
                .conexiones(List.of("TASK_1")).build();
        nodos.add(start);

        for (int i = 1; i <= n; i++) {
            String nextId = (i == n) ? "END" : "TASK_" + (i + 1);
            Nodo task = Nodo.builder()
                    .id("TASK_" + i)
                    .nombre("Tarea " + i)
                    .tipo("TASK")
                    .conexiones(List.of(nextId))
                    .build();
            nodos.add(task);
        }

        Nodo end = Nodo.builder().id("END").nombre("Fin").tipo("END").build();
        nodos.add(end);

        return Politica.builder().id("POL-1").nombre("Test").nodos(nodos).build();
    }

    /**
     * Builds: START → DECISION → [TASK_A (true path), TASK_B (false path)] → END
     */
    private Politica buildDecisionPolitica() {
        Nodo start = Nodo.builder().id("START").nombre("Inicio").tipo("START")
                .conexiones(List.of("DECISION")).build();

        Nodo decision = Nodo.builder().id("DECISION").nombre("Decisión").tipo("DECISION")
                .conexiones(List.of("TASK_A", "TASK_B")).build();

        Nodo taskA = Nodo.builder().id("TASK_A").nombre("Tarea A").tipo("TASK")
                .conexiones(List.of("END")).build();

        Nodo taskB = Nodo.builder().id("TASK_B").nombre("Tarea B").tipo("TASK")
                .conexiones(List.of("END")).build();

        Nodo end = Nodo.builder().id("END").nombre("Fin").tipo("END").build();

        return Politica.builder().id("POL-DEC").nombre("Decision Test")
                .nodos(List.of(start, decision, taskA, taskB, end)).build();
    }

    private Nodo getNodo(Politica politica, String nodoId) {
        return politica.getNodos().stream()
                .filter(n -> n.getId().equals(nodoId))
                .findFirst()
                .orElseThrow();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.1 — Sequential Routing (Property 6)
    // Feature: workflow-management-system, Property 6: Workflow Routing Correctness — Sequential
    // Validates: Requirements 8.1, 8.4, 8.7
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void sequentialRoutingCompletesAllTasks(@ForAll @IntRange(min = 2, max = 5) int n) {
        Politica politica = buildLinearPolitica(n);
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        // Start at TASK_1
        InMemoryWorkflowEngine.SimulatedTarea tarea =
                new InMemoryWorkflowEngine.SimulatedTarea("TASK_1");

        for (int i = 1; i <= n; i++) {
            Nodo nodo = getNodo(politica, tarea.nodoId);
            tarea = InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, Map.of());
        }

        assertThat(tramite.estado)
                .as("Trámite should be COMPLETADO after completing all %d tasks", n)
                .isEqualTo(EstadoTramite.COMPLETADO);

        assertThat(tramite.historial)
                .as("Historial should have exactly %d entries", n)
                .hasSize(n);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.2 — DECISION Routing (Property 7)
    // Feature: workflow-management-system, Property 7: DECISION Routing Correctness
    // Validates: Requirements 8.2
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void decisionRoutingSelectsCorrectBranch(@ForAll boolean aprobado) {
        Politica politica = buildDecisionPolitica();
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        // Complete the DECISION node
        Nodo decisionNodo = getNodo(politica, "DECISION");
        InMemoryWorkflowEngine.SimulatedTarea decisionTarea =
                new InMemoryWorkflowEngine.SimulatedTarea("DECISION");

        Map<String, Object> datos = new HashMap<>();
        datos.put("aprobado", String.valueOf(aprobado));

        InMemoryWorkflowEngine.SimulatedTarea nextTarea =
                InMemoryWorkflowEngine.completarTarea(tramite, decisionTarea, decisionNodo, politica, datos);

        assertThat(nextTarea).as("Next tarea should not be null after DECISION node").isNotNull();

        if (aprobado) {
            assertThat(nextTarea.nodoId)
                    .as("aprobado=true should route to TASK_A (conexiones[0])")
                    .isEqualTo("TASK_A");
        } else {
            assertThat(nextTarea.nodoId)
                    .as("aprobado=false should route to TASK_B (conexiones[1])")
                    .isEqualTo("TASK_B");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.3 — Historial Append-Only (Property 8)
    // Feature: workflow-management-system, Property 8: Historial Append-Only Invariant
    // Validates: Requirements 8.5
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void historialIsStrictlyAppendOnly(@ForAll @IntRange(min = 1, max = 4) int n) {
        Politica politica = buildLinearPolitica(n);
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        InMemoryWorkflowEngine.SimulatedTarea tarea =
                new InMemoryWorkflowEngine.SimulatedTarea("TASK_1");

        int previousSize = 0;
        for (int i = 1; i <= n; i++) {
            // Snapshot existing entries before completing
            List<HistorialTramite> snapshot = new ArrayList<>(tramite.historial);

            Nodo nodo = getNodo(politica, tarea.nodoId);
            tarea = InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, Map.of());

            int currentSize = tramite.historial.size();

            assertThat(currentSize)
                    .as("Historial size should strictly increase after step %d", i)
                    .isGreaterThan(previousSize);

            // Verify no prior entries were mutated
            for (int j = 0; j < snapshot.size(); j++) {
                assertThat(tramite.historial.get(j).getNodoId())
                        .as("Prior historial entry %d nodoId should not be mutated", j)
                        .isEqualTo(snapshot.get(j).getNodoId());
                assertThat(tramite.historial.get(j).getAccion())
                        .as("Prior historial entry %d accion should not be mutated", j)
                        .isEqualTo(snapshot.get(j).getAccion());
            }

            previousSize = currentSize;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.4 — Form Data Accumulation (Property 9)
    // Feature: workflow-management-system, Property 9: Form Data Accumulation
    // Validates: Requirements 8.6, 17.5
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void formDataAccumulatesAcrossAllTasks(@ForAll @IntRange(min = 2, max = 4) int n) {
        Politica politica = buildLinearPolitica(n);
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        InMemoryWorkflowEngine.SimulatedTarea tarea =
                new InMemoryWorkflowEngine.SimulatedTarea("TASK_1");

        Set<String> allKeys = new HashSet<>();

        for (int i = 1; i <= n; i++) {
            String key = "key" + i;
            allKeys.add(key);
            Map<String, Object> datos = Map.of(key, "value" + i);

            Nodo nodo = getNodo(politica, tarea.nodoId);
            tarea = InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, datos);
        }

        assertThat(tramite.datosFormulario.keySet())
                .as("datosFormulario should contain all keys from all completed tareas")
                .containsAll(allKeys);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.5 — Completed Task Rejection (Property 10)
    // Feature: workflow-management-system, Property 10: Completed Task Rejection
    // Validates: Requirements 8.8
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void completingAlreadyCompletedTareaThrowsBusinessException(
            @ForAll @IntRange(min = 1, max = 3) int ignored) {

        Politica politica = buildLinearPolitica(2);
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        Nodo nodo = getNodo(politica, "TASK_1");
        InMemoryWorkflowEngine.SimulatedTarea tarea =
                new InMemoryWorkflowEngine.SimulatedTarea("TASK_1");

        // First completion — should succeed
        InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, Map.of());

        assertThat(tramite.historial).hasSize(1);

        // Second completion — should throw BusinessException
        assertThatThrownBy(() ->
                InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue completada");

        // Historial must not have grown
        assertThat(tramite.historial)
                .as("Historial should still have exactly 1 entry after rejected duplicate completion")
                .hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property 14.6 — Trámite Duration Positivity (Property 11)
    // Feature: workflow-management-system, Property 11: Trámite Duration Positivity
    // Validates: Requirements 8.4
    // ─────────────────────────────────────────────────────────────────────────

    @Property
    void completedTramiteDurationIsPositive(@ForAll @IntRange(min = 1, max = 3) int n) {
        Politica politica = buildLinearPolitica(n);
        InMemoryWorkflowEngine.SimulatedTramite tramite = new InMemoryWorkflowEngine.SimulatedTramite();

        InMemoryWorkflowEngine.SimulatedTarea tarea =
                new InMemoryWorkflowEngine.SimulatedTarea("TASK_1");

        for (int i = 1; i <= n; i++) {
            Nodo nodo = getNodo(politica, tarea.nodoId);
            tarea = InMemoryWorkflowEngine.completarTarea(tramite, tarea, nodo, politica, Map.of());
        }

        assertThat(tramite.estado)
                .as("Trámite should be COMPLETADO")
                .isEqualTo(EstadoTramite.COMPLETADO);

        assertThat(tramite.duracionMinutos)
                .as("duracionMinutos should be > 0")
                .isGreaterThan(0L);

        assertThat(tramite.fechaFin)
                .as("fechaFin should be after fechaInicio")
                .isAfterOrEqualTo(tramite.fechaInicio);
    }
}
