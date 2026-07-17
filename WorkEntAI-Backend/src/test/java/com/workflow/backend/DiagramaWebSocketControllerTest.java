package com.workflow.backend;

import com.workflow.backend.dto.DiagramaCambioDTO;
import com.workflow.backend.websocket.DiagramaWebSocketController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiagramaWebSocketController.
 * Validates Requirement 4.9: real-time collaborative diagram editing via WebSocket.
 */
class DiagramaWebSocketControllerTest {

    private DiagramaWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new DiagramaWebSocketController();
    }

    @Test
    void procesarCambio_returnsSameInstance() {
        DiagramaCambioDTO cambio = new DiagramaCambioDTO("AGREGAR_NODO", "user-1", null, null, null, 10.0, 20.0);

        DiagramaCambioDTO result = controller.procesarCambio("politica-1", cambio);

        assertSame(cambio, result);
    }

    @Test
    void procesarCambio_tipoAgregarNodo_preservesAllFields() {
        DiagramaCambioDTO cambio = new DiagramaCambioDTO("AGREGAR_NODO", "editor-42", null, null, null, 100.0, 200.0);

        DiagramaCambioDTO result = controller.procesarCambio("politica-1", cambio);

        assertEquals("AGREGAR_NODO", result.tipo());
        assertEquals("editor-42", result.editorId());
        assertEquals(100.0, result.posX());
        assertEquals(200.0, result.posY());
    }

    @Test
    void procesarCambio_tipoMoverNodo_preservesAllFields() {
        DiagramaCambioDTO cambio = new DiagramaCambioDTO("MOVER_NODO", "editor-7", null, null, null, 50.5, 75.3);

        DiagramaCambioDTO result = controller.procesarCambio("politica-2", cambio);

        assertEquals("MOVER_NODO", result.tipo());
        assertEquals("editor-7", result.editorId());
        assertEquals(50.5, result.posX());
        assertEquals(75.3, result.posY());
    }

    @Test
    void procesarCambio_tipoConectar_preservesAllFields() {
        DiagramaCambioDTO cambio = new DiagramaCambioDTO("CONECTAR", "editor-3", null, "nodo-A", "nodo-B", 0.0, 0.0);

        DiagramaCambioDTO result = controller.procesarCambio("politica-3", cambio);

        assertEquals("CONECTAR", result.tipo());
        assertEquals("editor-3", result.editorId());
        assertEquals("nodo-A", result.desdeId());
        assertEquals("nodo-B", result.hastaId());
    }

    @Test
    void procesarCambio_differentPoliticaIds_returnsSameCambio() {
        DiagramaCambioDTO cambio = new DiagramaCambioDTO("AGREGAR_NODO", "editor-1", null, null, null, 0.0, 0.0);

        DiagramaCambioDTO result1 = controller.procesarCambio("politica-1", cambio);
        DiagramaCambioDTO result2 = controller.procesarCambio("politica-99", cambio);

        assertSame(cambio, result1);
        assertSame(cambio, result2);
    }
}
