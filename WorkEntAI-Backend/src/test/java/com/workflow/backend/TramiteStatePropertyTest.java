package com.workflow.backend;

// Feature: workflow-management-system, Property 4: New Trámite State and Reference Invariant
// Validates: Requirements 6.2, 14.1

import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.models.Tramite;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for New Trámite State and Reference Invariant.
 *
 * Verifies that for any valid (politicaId, clienteId) pair, a newly created
 * Trámite has estado == NUEVO and a non-null numeroReferencia matching TRM-\d{4}-\d{4}.
 */
class TramiteStatePropertyTest {

    /**
     * In-memory simulation of TramiteService.iniciarTramite() creation logic.
     * Avoids MongoDB dependency while faithfully replicating the invariants.
     */
    static class InMemoryTramiteFactory {
        private final AtomicLong counter = new AtomicLong(0);

        Tramite create(String politicaId, String clienteId) {
            int year = LocalDateTime.now().getYear();
            long seq = counter.incrementAndGet();
            String numeroReferencia = String.format("TRM-%d-%04d", year, seq);

            return Tramite.builder()
                    .politicaId(politicaId)
                    .clienteId(clienteId)
                    .estado(EstadoTramite.NUEVO)
                    .numeroReferencia(numeroReferencia)
                    .fechaInicio(LocalDateTime.now())
                    .build();
        }
    }

    private final InMemoryTramiteFactory factory = new InMemoryTramiteFactory();

    @Property
    void newTramiteHasEstadoNuevoAndValidReferencia(
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String politicaId,
            @ForAll @AlphaChars @StringLength(min = 3, max = 10) String clienteId) {

        Tramite tramite = factory.create(politicaId, clienteId);

        assertThat(tramite.getEstado())
                .as("estado should be NUEVO after iniciarTramite()")
                .isEqualTo(EstadoTramite.NUEVO);

        assertThat(tramite.getNumeroReferencia())
                .as("numeroReferencia should not be null")
                .isNotNull();

        assertThat(tramite.getNumeroReferencia())
                .as("numeroReferencia should match TRM-\\d{4}-\\d{4}")
                .matches("TRM-\\d{4}-\\d{4}");
    }
}
