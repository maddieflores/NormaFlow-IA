package com.workflow.backend;

import com.workflow.backend.models.Notificacion;
import com.workflow.backend.repositories.NotificacionRepository;
import com.workflow.backend.repositories.UsuarioRepository;
import com.workflow.backend.services.FcmService;
import com.workflow.backend.services.NotificacionService;
import com.workflow.backend.websocket.NotificacionWebSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificacionService.
 * Validates: Requirements 10.4
 */
@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private NotificacionWebSocket notificacionWebSocket;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private FcmService fcmService;

    @InjectMocks
    private NotificacionService notificacionService;

    // -------------------------------------------------------------------------
    // notificarFuncionario
    // -------------------------------------------------------------------------

    @Test
    void notificarFuncionario_persistsNotificacionWithCorrectFields() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

        notificacionService.notificarFuncionario(
                "func-1", "Tienes una nueva tarea", "NUEVA_TAREA", "tarea-99", "TAREA");

        verify(notificacionRepository).save(captor.capture());
        Notificacion saved = captor.getValue();

        assertThat(saved.getUsuarioId()).isEqualTo("func-1");
        assertThat(saved.getMensaje()).isEqualTo("Tienes una nueva tarea");
        assertThat(saved.getTipo()).isEqualTo("NUEVA_TAREA");
        assertThat(saved.getReferenciaId()).isEqualTo("tarea-99");
        assertThat(saved.getTipoReferencia()).isEqualTo("TAREA");
        assertThat(saved.getFechaCreacion()).isNotNull();
    }

    @Test
    void notificarFuncionario_dispatchesWebSocketToCorrectTopic() {
        notificacionService.notificarFuncionario(
                "func-1", "Tienes una nueva tarea", "NUEVA_TAREA", "tarea-99", "TAREA");

        verify(notificacionWebSocket).notificarFuncionario(eq("func-1"), eq("Tienes una nueva tarea"));
    }

    @Test
    void notificarFuncionario_doesNothingWhenFuncionarioIdIsNull() {
        notificacionService.notificarFuncionario(null, "msg", "SISTEMA", null, null);

        verifyNoInteractions(notificacionRepository, notificacionWebSocket);
    }

    // -------------------------------------------------------------------------
    // notificarCliente
    // -------------------------------------------------------------------------

    @Test
    void notificarCliente_persistsNotificacionWithCorrectFields() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

        notificacionService.notificarCliente(
                "cliente-42", "Su trámite fue completado", "TRAMITE_COMPLETADO", "tramite-7", "TRAMITE");

        verify(notificacionRepository).save(captor.capture());
        Notificacion saved = captor.getValue();

        assertThat(saved.getUsuarioId()).isEqualTo("cliente-42");
        assertThat(saved.getMensaje()).isEqualTo("Su trámite fue completado");
        assertThat(saved.getTipo()).isEqualTo("TRAMITE_COMPLETADO");
        assertThat(saved.getReferenciaId()).isEqualTo("tramite-7");
        assertThat(saved.getTipoReferencia()).isEqualTo("TRAMITE");
        assertThat(saved.getFechaCreacion()).isNotNull();
    }

    @Test
    void notificarCliente_dispatchesWebSocketToCorrectTopic() {
        notificacionService.notificarCliente(
                "cliente-42", "Su trámite fue completado", "TRAMITE_COMPLETADO", "tramite-7", "TRAMITE");

        verify(notificacionWebSocket).notificarCliente(eq("cliente-42"), eq("Su trámite fue completado"));
    }

    @Test
    void notificarCliente_doesNothingWhenClienteIdIsNull() {
        notificacionService.notificarCliente(null, "msg", "SISTEMA", null, null);

        verifyNoInteractions(notificacionRepository, notificacionWebSocket);
    }

    // -------------------------------------------------------------------------
    // notificarAdmin
    // -------------------------------------------------------------------------

    @Test
    void notificarAdmin_persistsNotificacionWithAdminUsuarioId() {
        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);

        notificacionService.notificarAdmin("Cuello de botella detectado", "CUELLO_BOTELLA", "tramite-3", "TRAMITE");

        verify(notificacionRepository).save(captor.capture());
        Notificacion saved = captor.getValue();

        assertThat(saved.getUsuarioId()).isEqualTo("admin");
        assertThat(saved.getMensaje()).isEqualTo("Cuello de botella detectado");
        assertThat(saved.getTipo()).isEqualTo("CUELLO_BOTELLA");
        assertThat(saved.getReferenciaId()).isEqualTo("tramite-3");
        assertThat(saved.getTipoReferencia()).isEqualTo("TRAMITE");
        assertThat(saved.getFechaCreacion()).isNotNull();
    }

    @Test
    void notificarAdmin_dispatchesWebSocketToAdminTopic() {
        notificacionService.notificarAdmin("Cuello de botella detectado", "CUELLO_BOTELLA", "tramite-3", "TRAMITE");

        verify(notificacionWebSocket).notificarAdmin(eq("Cuello de botella detectado"));
    }

    // -------------------------------------------------------------------------
    // notificarDepartamento
    // -------------------------------------------------------------------------

    @Test
    void notificarDepartamento_dispatchesWebSocketToCorrectDepartamentoTopic() {
        notificacionService.notificarDepartamento(
                "finanzas", "Nuevo trámite asignado", "NUEVA_TAREA", "tramite-5", "TRAMITE");

        verify(notificacionWebSocket).notificarDepartamento(eq("finanzas"), eq("Nuevo trámite asignado"));
    }

    @Test
    void notificarDepartamento_doesNotPersistNotificacion() {
        notificacionService.notificarDepartamento(
                "finanzas", "Nuevo trámite asignado", "NUEVA_TAREA", "tramite-5", "TRAMITE");

        verifyNoInteractions(notificacionRepository);
    }
}
