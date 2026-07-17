package com.workflow.backend.controllers;

import com.workflow.backend.ai.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/diagrama")
    public ResponseEntity<?> procesarDiagrama(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(aiService.procesarPromptDiagrama(body.get("prompt")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servicio de IA no disponible: " + e.getMessage()));
        }
    }

    @PostMapping("/extraer-datos")
    public ResponseEntity<?> extraerDatos(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(aiService.extraerDatosDocumento(
                    body.get("texto"), body.get("nombreNodo")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servicio de IA no disponible: " + e.getMessage()));
        }
    }

    @GetMapping("/cuellos-botella/{politicaId}")
    public ResponseEntity<?> detectarCuellos(@PathVariable String politicaId) {
        try {
            return ResponseEntity.ok(aiService.detectarCuellosBottella(politicaId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servicio de IA no disponible: " + e.getMessage()));
        }
    }

    @PostMapping("/asistente")
    public ResponseEntity<?> asistente(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(aiService.asistente(body.get("pregunta")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servicio de IA no disponible: " + e.getMessage()));
        }
    }

    @GetMapping("/plantuml/{politicaId}")
    public ResponseEntity<?> generarPlantUML(@PathVariable String politicaId) {
        try {
            String plantuml = aiService.generarPlantUML(politicaId);
            return ResponseEntity.ok(Map.of("plantuml", plantuml));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Servicio de IA no disponible: " + e.getMessage()));
        }
    }
}