package com.workflow.backend.controllers;

import com.workflow.backend.models.Departamento;
import com.workflow.backend.services.DepartamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departamentos")
@RequiredArgsConstructor
public class DepartamentoController {

    private final DepartamentoService departamentoService;

    @GetMapping
    public ResponseEntity<List<Departamento>> getAll() {
        return ResponseEntity.ok(departamentoService.getAll());
    }

    @PostMapping
    public ResponseEntity<Departamento> create(@RequestBody Departamento departamento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departamentoService.create(departamento));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Departamento> update(@PathVariable String id, @RequestBody Departamento departamento) {
        return ResponseEntity.ok(departamentoService.update(id, departamento));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        departamentoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
