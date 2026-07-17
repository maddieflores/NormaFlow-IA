package com.workflow.backend.controllers;

import com.workflow.backend.models.Notificacion;
import com.workflow.backend.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionRepository notificacionRepository;

    // Todas las notificaciones del usuario
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Notificacion>> getByUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(notificacionRepository.findByUsuarioId(usuarioId));
    }

    // Solo las no leídas (para el badge del frontend)
    @GetMapping("/usuario/{usuarioId}/no-leidas")
    public ResponseEntity<List<Notificacion>> getNoLeidas(@PathVariable String usuarioId) {
        return ResponseEntity.ok(notificacionRepository.findByUsuarioIdAndLeida(usuarioId, false));
    }

    // Conteo de no leídas (para el badge numérico)
    @GetMapping("/usuario/{usuarioId}/conteo")
    public ResponseEntity<Map<String, Long>> getConteo(@PathVariable String usuarioId) {
        long count = notificacionRepository.findByUsuarioIdAndLeida(usuarioId, false).size();
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }

    // Marcar una notificación como leída
    @PutMapping("/{id}/leer")
    public ResponseEntity<Notificacion> marcarLeida(@PathVariable String id) {
        return notificacionRepository.findById(id).map(n -> {
            n.setLeida(true);
            n.setFechaLeida(LocalDateTime.now());
            return ResponseEntity.ok(notificacionRepository.save(n));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Marcar todas como leídas
    @PutMapping("/usuario/{usuarioId}/leer-todas")
    public ResponseEntity<Void> marcarTodasLeidas(@PathVariable String usuarioId) {
        List<Notificacion> pendientes = notificacionRepository.findByUsuarioIdAndLeida(usuarioId, false);
        LocalDateTime ahora = LocalDateTime.now();
        pendientes.forEach(n -> {
            n.setLeida(true);
            n.setFechaLeida(ahora);
        });
        notificacionRepository.saveAll(pendientes);
        return ResponseEntity.ok().build();
    }
}
