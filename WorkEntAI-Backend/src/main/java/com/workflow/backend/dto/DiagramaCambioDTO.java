package com.workflow.backend.dto;

import com.workflow.backend.models.Nodo;

public record DiagramaCambioDTO(
        String tipo,
        String editorId,
        Nodo nodo,
        String desdeId,
        String hastaId,
        double posX,
        double posY,
        String modelo,
        String editorNombre,
        Integer editoresActivos,
        String nombresEditores) {
    public DiagramaCambioDTO(String tipo, String editorId, Nodo nodo, String desdeId, String hastaId, double posX,
            double posY) {
        this(tipo, editorId, nodo, desdeId, hastaId, posX, posY, null, null, null, null);
    }
}
