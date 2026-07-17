package com.workflow.backend.controllers;

import com.workflow.backend.services.PredictorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller del Motor Predictivo (Ciclo 2 — CU-28/29).
 *
 * Endpoints:
 *   GET /api/predictor/ruta/{politicaId}   — ruta óptima con análisis estadístico + IA
 *   GET /api/predictor/riesgo/{tramiteId}  — score de riesgo del trámite activo
 *
 * Principio SRP: solo orquesta HTTP; la lógica está en PredictorService.
 */
@RestController
@RequestMapping("/api/predictor")
@RequiredArgsConstructor
public class PredictorController {

    private final PredictorService predictorService;

    /**
     * Analiza el histórico de una política y predice la ruta más eficiente.
     * Incluye estadísticas por nodo y recomendaciones generadas por IA.
     * Solo accesible para ADMIN y FUNCIONARIO.
     */
    @GetMapping("/ruta/{politicaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<Map<String, Object>> predecirRutaOptima(
            @PathVariable String politicaId) {
        return ResponseEntity.ok(predictorService.predecirRutaOptima(politicaId));
    }

    /**
     * Calcula el score de riesgo de demora para un trámite activo.
     * Score 0-100: BAJO (<45) | MEDIO (45-74) | ALTO (≥75).
     * Accesible por todos los roles autenticados (el cliente puede ver su trámite).
     */
    @GetMapping("/riesgo/{tramiteId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> calcularRiesgo(@PathVariable String tramiteId) {
        return ResponseEntity.ok(predictorService.calcularRiesgoDemora(tramiteId));
    }

    /**
     * Obtiene las anomalías activas detectadas por la IA (CU-29).
     */
    @GetMapping("/anomalias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAnomalias() {
        return ResponseEntity.ok(predictorService.getAnomaliasActivas());
    }

    /**
     * Resuelve una anomalía (CU-29).
     */
    @PostMapping("/anomalias/{id}/resolver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resolverAnomalia(@PathVariable String id) {
        predictorService.resolverAnomalia(id);
        return ResponseEntity.ok().build();
    }
}
