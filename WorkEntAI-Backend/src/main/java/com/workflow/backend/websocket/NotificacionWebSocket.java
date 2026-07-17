package com.workflow.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificacionWebSocket {

    private final SimpMessagingTemplate messagingTemplate;

    public void notificarFuncionario(String funcionarioId, String mensaje) {
        messagingTemplate.convertAndSend(
                "/topic/funcionario/" + funcionarioId, mensaje
        );
    }

    public void notificarCliente(String clienteId, String mensaje) {
        messagingTemplate.convertAndSend(
                "/topic/cliente/" + clienteId, mensaje
        );
    }

    public void notificarAdmin(String mensaje) {
        messagingTemplate.convertAndSend("/topic/admin", mensaje);
    }

    public void notificarDepartamento(String departamento, String mensaje) {
        messagingTemplate.convertAndSend(
                "/topic/departamento/" + departamento, mensaje
        );
    }
}