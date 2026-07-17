package com.workflow.backend.services;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.models.*;
import com.workflow.backend.repositories.PoliticaRepository;
import com.workflow.backend.repositories.TareaRepository;
import com.workflow.backend.repositories.TramiteRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WorkflowEngineServiceTest {

    @Mock
    private TareaRepository tareaRepository;

    @Mock
    private TramiteRepository tramiteRepository;

    @Mock
    private PoliticaRepository politicaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private CondicionEvaluador condicionEvaluador;

    @InjectMocks
    private WorkflowEngineService workflowEngineService;

    private Tarea tareaActual;
    private Tramite tramiteActual;
    private Politica politicaActual;

    @BeforeEach
    void setUp() {
        tareaActual = Tarea.builder()
                .id("t1")
                .tramiteId("tram1")
                .nodoId("n1")
                .estado(EstadoTarea.PENDIENTE)
                .fechaAsignacion(LocalDateTime.now().minusMinutes(10))
                .build();

        tramiteActual = Tramite.builder()
                .id("tram1")
                .politicaId("p1")
                .numeroReferencia("TRAM-001")
                .estado(EstadoTramite.EN_PROCESO)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .build();

        Nodo nodo1 = Nodo.builder()
                .id("n1")
                .nombre("Revisión Inicial")
                .tipo("TASK")
                .conexiones(List.of("n2"))
                .build();

        Nodo nodo2 = Nodo.builder()
                .id("n2")
                .nombre("Aprobación")
                .tipo("TASK")
                .conexiones(List.of("end1"))
                .build();

        Nodo nodoEnd = Nodo.builder()
                .id("end1")
                .tipo("END")
                .build();

        politicaActual = Politica.builder()
                .id("p1")
                .nombre("Política Test")
                .nodos(List.of(nodo1, nodo2, nodoEnd))
                .build();
    }

    @Test
    void testCompletarTareaAvanzaAlSiguienteNodo() {
        when(tareaRepository.findById("t1")).thenReturn(Optional.of(tareaActual));
        when(tramiteRepository.findById("tram1")).thenReturn(Optional.of(tramiteActual));
        when(politicaRepository.findById("p1")).thenReturn(Optional.of(politicaActual));

        Map<String, Object> form = new HashMap<>();
        form.put("observacion", "Todo correcto");

        workflowEngineService.completarTarea("t1", form);

        // Verifica que la tarea se completó
        assertEquals(EstadoTarea.COMPLETADO, tareaActual.getEstado());
        verify(tareaRepository).save(tareaActual);

        // Verifica que se crea nueva tarea para el nodo 2
        verify(tareaRepository).save(argThat(t -> t.getNodoId().equals("n2")));

        // Verifica que el trámite avanza al nodo 2
        assertEquals("n2", tramiteActual.getNodoActualId());
        verify(tramiteRepository).save(tramiteActual);
    }

    @Test
    void testCompletarTareaAvanzaAEnd() {
        // Configuramos para que esté en el nodo 2 que conecta a END
        tareaActual.setNodoId("n2");
        when(tareaRepository.findById("t1")).thenReturn(Optional.of(tareaActual));
        when(tramiteRepository.findById("tram1")).thenReturn(Optional.of(tramiteActual));
        when(politicaRepository.findById("p1")).thenReturn(Optional.of(politicaActual));

        workflowEngineService.completarTarea("t1", new HashMap<>());

        // Verifica que el trámite se completó
        assertEquals(EstadoTramite.COMPLETADO, tramiteActual.getEstado());
        assertNotNull(tramiteActual.getFechaFin());
        verify(tramiteRepository).save(tramiteActual);
    }
}
