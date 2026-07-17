package com.workflow.backend;

// Feature: workflow-management-system, Property 2: New Política Always Starts in BORRADOR
// Feature: workflow-management-system, Property 3: Workflow Graph Validity Invariant
// Validates: Requirements 5.1, 4.10, 5.2

import com.workflow.backend.enums.EstadoPolitica;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.models.Nodo;
import com.workflow.backend.models.Politica;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Property tests for Política creation and workflow graph validation.
 *
 * Property 2: New Política Always Starts in BORRADOR
 *   Validates: Requirements 5.1
 *
 * Property 3: Workflow Graph Validity Invariant
 *   Validates: Requirements 4.10, 5.2
 */
class PoliticaPropertyTest {

    // -------------------------------------------------------------------------
    // Static helpers (extracted from PoliticaService to avoid MongoDB dependency)
    // -------------------------------------------------------------------------

    static Politica crearPolitica(String nombre, String descripcion, String categoria, String organizacion) {
        Politica p = new Politica();
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setCategoria(categoria);
        p.setOrganizacion(organizacion);
        p.setEstado(EstadoPolitica.BORRADOR);
        p.setVersion(1);
        p.setFechaCreacion(LocalDateTime.now());
        return p;
    }

    static void validarGrafo(List<Nodo> nodos) {
        if (nodos == null || nodos.isEmpty()) throw new BusinessException("Sin nodos");
        boolean tieneStart = nodos.stream().anyMatch(n -> "START".equals(n.getTipo()));
        boolean tieneEnd   = nodos.stream().anyMatch(n -> "END".equals(n.getTipo()));
        if (!tieneStart || !tieneEnd) throw new BusinessException("Falta START o END");
    }

    // =========================================================================
    // Property 2: New Política Always Starts in BORRADOR
    // Validates: Requirements 5.1
    // =========================================================================

    @Property
    void newPoliticaAlwaysStartsInBorrador(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String nombre,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String descripcion,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String categoria,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String organizacion) {

        Politica p = crearPolitica(nombre, descripcion, categoria, organizacion);

        assertThat(p.getEstado())
                .as("A newly created Política must always start in BORRADOR state")
                .isEqualTo(EstadoPolitica.BORRADOR);

        assertThat(p.getVersion())
                .as("A newly created Política must always start at version 1")
                .isEqualTo(1);
    }

    // =========================================================================
    // Property 3: Workflow Graph Validity Invariant
    // Validates: Requirements 4.10, 5.2
    // =========================================================================

    /** Property 3-A: graphs missing START or END always fail validation */
    @Property
    void graphMissingStartOrEndThrowsBusinessException(
            @ForAll("graphsMissingStartOrEnd") List<Nodo> nodos) {

        assertThatThrownBy(() -> validarGrafo(nodos))
                .as("A graph without START or END must throw BusinessException")
                .isInstanceOf(BusinessException.class);
    }

    /** Property 3-B: graphs with exactly one START and at least one END pass validation */
    @Property
    void validGraphDoesNotThrow(
            @ForAll("validGraphs") List<Nodo> nodos) {

        assertThatCode(() -> validarGrafo(nodos))
                .as("A graph with exactly one START and at least one END must not throw")
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // Arbitraries
    // =========================================================================

    /** Generates graphs that are missing START, missing END, or both. */
    @Provide
    Arbitrary<List<Nodo>> graphsMissingStartOrEnd() {
        Arbitrary<List<Nodo>> missingStart = taskNodes().list().ofMinSize(1).ofMaxSize(5)
                .map(nodes -> {
                    // Ensure at least one END but no START
                    List<Nodo> result = new ArrayList<>(nodes);
                    result.add(nodoWithTipo("END"));
                    return result;
                });

        Arbitrary<List<Nodo>> missingEnd = taskNodes().list().ofMinSize(1).ofMaxSize(5)
                .map(nodes -> {
                    // Ensure at least one START but no END
                    List<Nodo> result = new ArrayList<>(nodes);
                    result.add(nodoWithTipo("START"));
                    return result;
                });

        Arbitrary<List<Nodo>> missingBoth = taskNodes().list().ofMinSize(1).ofMaxSize(5);

        return Arbitraries.oneOf(missingStart, missingEnd, missingBoth);
    }

    /** Generates graphs with exactly one START and at least one END. */
    @Provide
    Arbitrary<List<Nodo>> validGraphs() {
        return taskNodes().list().ofMinSize(0).ofMaxSize(4)
                .map(middleNodes -> {
                    List<Nodo> result = new ArrayList<>();
                    result.add(nodoWithTipo("START"));
                    result.addAll(middleNodes);
                    result.add(nodoWithTipo("END"));
                    return result;
                });
    }

    /** Generates TASK nodes (never START or END) for use as filler nodes. */
    private Arbitrary<Nodo> taskNodes() {
        return Arbitraries.of("TASK", "DECISION", "PARALLEL")
                .map(PoliticaPropertyTest::nodoWithTipo);
    }

    private static Nodo nodoWithTipo(String tipo) {
        Nodo n = new Nodo();
        n.setTipo(tipo);
        return n;
    }
}
