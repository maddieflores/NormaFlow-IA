package com.workflow.backend.services;

import com.workflow.backend.models.Notificacion;
import com.workflow.backend.repositories.NotificacionRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.websocket.NotificacionWebSocket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Centralizes notification persistence, WebSocket dispatch, and FCM push.
 * Requirements: 10.1, 10.2, 10.3, 10.4
 */
@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionWebSocket notificacionWebSocket;
    private final UsuarioRepository usuarioRepository;
    private final FcmService fcmService;

    /** Persists + WebSocket + FCM push para un funcionario. */
    public void notificarFuncionario(String funcionarioId, String mensaje, String tipo,
                                     String referenciaId, String tipoReferencia) {
        if (funcionarioId == null) return;
        persistir(funcionarioId, mensaje, tipo, referenciaId, tipoReferencia);
        notificacionWebSocket.notificarFuncionario(funcionarioId, mensaje);
        enviarPush(funcionarioId, "Nueva tarea asignada", mensaje, tipo, referenciaId);
    }

    /** Persists + WebSocket + FCM push para un cliente. */
    public void notificarCliente(String clienteId, String mensaje, String tipo,
                                 String referenciaId, String tipoReferencia) {
        if (clienteId == null) return;
        persistir(clienteId, mensaje, tipo, referenciaId, tipoReferencia);
        notificacionWebSocket.notificarCliente(clienteId, mensaje);
        enviarPush(clienteId, getTituloPorTipo(tipo), mensaje, tipo, referenciaId);
    }

    /** Persists + WebSocket + FCM push para admin. */
    public void notificarAdmin(String mensaje, String tipo,
                               String referenciaId, String tipoReferencia) {
        persistir("admin", mensaje, tipo, referenciaId, tipoReferencia);
        notificacionWebSocket.notificarAdmin(mensaje);
        // Notificar a todos los admins con FCM token
        usuarioRepository.findByRol(com.workflow.backend.enums.RolUsuario.ADMIN)
            .forEach(admin -> enviarPush(admin.getId(), "WorkEntAI Admin", mensaje, tipo, referenciaId));
    }

    /** WebSocket broadcast para departamento (sin persistencia). */
    public void notificarDepartamento(String departamento, String mensaje, String tipo,
                                      String referenciaId, String tipoReferencia) {
        notificacionWebSocket.notificarDepartamento(departamento, mensaje);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void enviarPush(String usuarioId, String titulo, String mensaje,
                             String tipo, String referenciaId) {
        usuarioRepository.findById(usuarioId).ifPresent(u -> {
            if (u.getFcmToken() != null && !u.getFcmToken().isBlank()) {
                fcmService.enviarNotificacion(u.getFcmToken(), titulo, mensaje, tipo, referenciaId);
            }
        });
    }

    private String getTituloPorTipo(String tipo) {
        if (tipo == null) return "WorkEntAI";
        return switch (tipo) {
            case "TRAMITE_COMPLETADO" -> "✅ Trámite completado";
            case "TRAMITE_RECHAZADO"  -> "❌ Trámite rechazado";
            case "TRAMITE_AVANZADO"   -> "➡️ Trámite avanzó";
            case "CUELLO_BOTELLA"     -> "⚠️ Alerta de cuello de botella";
            default -> "WorkEntAI";
        };
    }

    private void persistir(String usuarioId, String mensaje, String tipo,
                            String referenciaId, String tipoReferencia) {
        Notificacion notif = Notificacion.builder()
                .usuarioId(usuarioId)
                .mensaje(mensaje)
                .tipo(tipo)
                .referenciaId(referenciaId)
                .tipoReferencia(tipoReferencia)
                .fechaCreacion(LocalDateTime.now())
                .build();
        notificacionRepository.save(notif);
    }
}
