package com.workflow.backend;

import com.workflow.backend.repositories.NotificacionRepository;
import com.workflow.backend.repositories.TareaRepository;
import com.workflow.backend.repositories.TramiteRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.repositories.PoliticaRepository;
import com.workflow.backend.repositories.DepartamentoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifica que el contexto de Spring arranca correctamente.
 * Usa @MockBean para evitar conexiones reales a MongoDB y servicios externos.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private TareaRepository tareaRepository;

    @MockitoBean
    private TramiteRepository tramiteRepository;

    @MockitoBean
    private PoliticaRepository politicaRepository;

    @MockitoBean
    private NotificacionRepository notificacionRepository;

    @MockitoBean
    private DepartamentoRepository departamentoRepository;

    @Test
    void contextLoads() {
        // El contexto debe cargar sin errores
    }
}
