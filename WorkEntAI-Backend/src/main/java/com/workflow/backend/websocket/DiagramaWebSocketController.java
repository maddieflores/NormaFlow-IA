package com.workflow.backend.websocket;

import com.workflow.backend.dto.DiagramaCambioDTO;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class DiagramaWebSocketController {

    private final Map<String, Map<String, String>> diagramSessions = new ConcurrentHashMap<>();

    @MessageMapping("/politica/{id}/editar")
    @SendTo("/topic/politica/{id}")
    public DiagramaCambioDTO procesarCambio(@DestinationVariable String id, DiagramaCambioDTO cambio) {
        if (cambio == null)
            return null;

        if ("JOIN".equals(cambio.tipo())) {
            diagramSessions.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            diagramSessions.get(id).put(cambio.editorId(),
                    cambio.editorNombre() != null ? cambio.editorNombre() : "Usuario Anónimo");
        } else if ("LEAVE".equals(cambio.tipo())) {
            if (diagramSessions.containsKey(id)) {
                diagramSessions.get(id).remove(cambio.editorId());
            }
        }

        int count = diagramSessions.containsKey(id) ? diagramSessions.get(id).size() : 0;
        String names = diagramSessions.containsKey(id) ? String.join(", ", diagramSessions.get(id).values()) : "";

        return new DiagramaCambioDTO(
                cambio.tipo(),
                cambio.editorId(),
                cambio.nodo(),
                cambio.desdeId(),
                cambio.hastaId(),
                cambio.posX(),
                cambio.posY(),
                cambio.modelo(),
                cambio.editorNombre(),
                count,
                names);
    }
}
