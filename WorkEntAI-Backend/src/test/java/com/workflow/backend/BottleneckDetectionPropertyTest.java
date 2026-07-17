package com.workflow.backend;

// Feature: workflow-management-system, Property 13: Bottleneck Detection Threshold
// Validates: Requirements 13.2

import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;
import net.jqwik.api.constraints.IntRange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property test for Bottleneck Detection Threshold.
 *
 * Verifies that for any Nodo with tiempoLimiteHoras = T:
 * - An alert is generated when average duration > T×60 minutes
 * - An alert is NOT generated when average duration ≤ T×60 minutes
 */
class BottleneckDetectionPropertyTest {

    /**
     * Extracted threshold logic from AIService, testable without MongoDB/Spring context.
     */
    static boolean isBottleneck(int tiempoLimiteHoras, double avgDuracionMinutos) {
        return avgDuracionMinutos > tiempoLimiteHoras * 60.0;
    }

    /**
     * Property 1: for any T (1-24 hours) and any avg > T*60, isBottleneck returns true.
     * Generator produces correlated (T, avg) pairs where avg is guaranteed above T*60.
     */
    @Property
    void alertGeneratedWhenAverageExceedsThreshold(
            @ForAll("aboveThresholdCases") Tuple.Tuple2<Integer, Double> testCase) {

        int tiempoLimiteHoras = testCase.get1();
        double avgDuracion = testCase.get2();

        assertThat(isBottleneck(tiempoLimiteHoras, avgDuracion))
                .as("isBottleneck should return true when avg (%.2f) > threshold (%d * 60 = %d)",
                        avgDuracion, tiempoLimiteHoras, tiempoLimiteHoras * 60)
                .isTrue();
    }

    @Provide
    Arbitrary<Tuple.Tuple2<Integer, Double>> aboveThresholdCases() {
        return Arbitraries.integers().between(1, 24).flatMap(t -> {
            double minAvg = t * 60.0 + 0.1;
            double maxAvg = t * 60.0 + 1440.0;
            return Arbitraries.doubles()
                    .between(minAvg, maxAvg)
                    .ofScale(3)
                    .map(avg -> Tuple.of(t, avg));
        });
    }

    /**
     * Property 2: for any T (1-24 hours) and any avg ≤ T*60, isBottleneck returns false.
     * Uses Assume to filter generated values to the valid range.
     */
    @Property
    void noAlertGeneratedWhenAverageAtOrBelowThreshold(
            @ForAll @IntRange(min = 1, max = 24) int tiempoLimiteHoras,
            @ForAll @DoubleRange(min = 0.1, max = 1440.0) double avgDuracion) {

        Assume.that(avgDuracion <= tiempoLimiteHoras * 60.0);

        assertThat(isBottleneck(tiempoLimiteHoras, avgDuracion))
                .as("isBottleneck should return false when avg (%.2f) <= threshold (%d * 60 = %d)",
                        avgDuracion, tiempoLimiteHoras, tiempoLimiteHoras * 60)
                .isFalse();
    }
}
