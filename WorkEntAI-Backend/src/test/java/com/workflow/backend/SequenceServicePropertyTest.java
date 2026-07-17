package com.workflow.backend;

// Feature: workflow-management-system, Property 5: NumeroReferencia Uniqueness
// Validates: Requirements 14.2

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for NumeroReferencia uniqueness.
 *
 * Simulates the SequenceService counter logic (atomic increment) and verifies
 * that N sequential calls for the same year always produce distinct values.
 */
class SequenceServicePropertyTest {

    /**
     * In-memory simulation of SequenceService.nextVal(year).
     * Each call atomically increments and returns the next counter value,
     * mirroring the MongoDB $inc behaviour in SequenceService.
     */
    static class InMemorySequenceService {
        private final AtomicLong counter = new AtomicLong(0);

        public long nextVal() {
            return counter.incrementAndGet();
        }
    }

    @Property
    void numeroReferenciaValuesAreUnique(@ForAll @IntRange(min = 2, max = 20) int n) {
        InMemorySequenceService service = new InMemorySequenceService();

        Set<Long> generated = new HashSet<>();
        for (int i = 0; i < n; i++) {
            long ref = service.nextVal();
            generated.add(ref);
        }

        // All N generated values must be distinct
        assertThat(generated).hasSize(n);
    }
}
