package com.workflow.backend.controllers;

import com.workflow.backend.services.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for analytics endpoints (ADMIN only).
 * Security is enforced via SecurityConfig.
 * Requirements: 13.3
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/nodos/{politicaId}")
    public ResponseEntity<Map<String, Double>> promediosPorNodo(@PathVariable String politicaId) {
        return ResponseEntity.ok(analyticsService.promediosPorNodo(politicaId));
    }

    @GetMapping("/departamentos")
    public ResponseEntity<Map<String, Double>> promediosPorDepartamento() {
        return ResponseEntity.ok(analyticsService.promediosPorDepartamento());
    }

    @GetMapping("/funcionarios/{dept}")
    public ResponseEntity<List<Map<String, Object>>> eficienciaFuncionarios(@PathVariable String dept) {
        return ResponseEntity.ok(analyticsService.eficienciaFuncionarios(dept));
    }

    @GetMapping("/kpis")
    public ResponseEntity<Map<String, Object>> kpisGlobales() {
        return ResponseEntity.ok(analyticsService.kpisGlobales());
    }
}
