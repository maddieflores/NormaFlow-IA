package com.workflow.backend.controllers;

import com.workflow.backend.models.Politica;
import com.workflow.backend.services.PoliticaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/politicas")
@RequiredArgsConstructor
public class PoliticaController {

    private final PoliticaService politicaService;

    @GetMapping
    public ResponseEntity<List<Politica>> getAll() {
        return ResponseEntity.ok(politicaService.getAll());
    }

    // Solo las activas — útil para que el cliente elija qué trámite iniciar
    @GetMapping("/activas")
    public ResponseEntity<List<Politica>> getActivas() {
        return ResponseEntity.ok(politicaService.getActivas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Politica> getById(@PathVariable String id) {
        return ResponseEntity.ok(politicaService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Politica> create(@RequestBody Politica politica) {
        return ResponseEntity.ok(politicaService.create(politica));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Politica> update(@PathVariable String id, @RequestBody Politica politica) {
        return ResponseEntity.ok(politicaService.update(id, politica));
    }

    @PutMapping("/{id}/activar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Politica> activar(@PathVariable String id) {
        return ResponseEntity.ok(politicaService.activar(id));
    }

    @PutMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Politica> desactivar(@PathVariable String id) {
        return ResponseEntity.ok(politicaService.desactivar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        politicaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}