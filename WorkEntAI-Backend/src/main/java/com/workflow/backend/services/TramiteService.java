package com.workflow.backend.services;

import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.*;
import com.workflow.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TramiteService {

    private final TramiteRepository tramiteRepository;
    private final PoliticaRepository politicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final SequenceService sequenceService;
    private final NotificacionService notificacionService;

    public List<Tramite> getAll() {
        return tramiteRepository.findAll();
    }

    public Tramite getById(String id) {
        return tramiteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trámite", id));
    }

    public List<Tramite> getByCliente(String clienteId) {
        return tramiteRepository.findByClienteId(clienteId);
    }

    public List<Tramite> getByEstado(EstadoTramite estado) {
        return tramiteRepository.findByEstado(estado);
    }

    public Tramite iniciarTramite(String politicaId, String clienteId, String descripcion) {
        return iniciarTramite(politicaId, clienteId, descripcion, "MEDIA");
    }

    public Tramite iniciarTramite(String politicaId, String clienteId, String descripcion, String prioridad) {
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Política", politicaId));

        if (politica.getEstado() != com.workflow.backend.enums.EstadoPolitica.ACTIVA) {
            throw new BusinessException("Solo se pueden iniciar trámites en políticas activas");
        }

        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));

        Nodo nodoInicio = politica.getNodos().stream()
                .filter(n -> "START".equals(n.getTipo()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("La política no tiene nodo de inicio (START)"));

        if (nodoInicio.getConexiones() == null || nodoInicio.getConexiones().isEmpty()) {
            throw new BusinessException("El nodo START no tiene conexiones definidas");
        }

        String primerNodoId = nodoInicio.getConexiones().get(0);
        Nodo primerNodo = getNodo(politica, primerNodoId);

        LocalDateTime ahora = LocalDateTime.now();
        String referencia = generarNumeroReferencia(ahora);

        Tramite tramite = Tramite.builder()
                .politicaId(politicaId)
                .nombrePolitica(politica.getNombre())
                .clienteId(clienteId)
                .nombreCliente(cliente.getNombre())
                .nodoActualId(primerNodoId)
                .nombreNodoActual(primerNodo.getNombre())
                .departamentoActual(primerNodo.getDepartamento())
                .estado(EstadoTramite.NUEVO)
                .descripcion(descripcion)
                .numeroReferencia(referencia)
                .prioridad(prioridad != null ? prioridad.toUpperCase() : "MEDIA")
                .historial(new ArrayList<>())
                .fechaInicio(ahora)
                .fechaUltimaActualizacion(ahora)
                .build();

        tramite = tramiteRepository.save(tramite);

        // Send WebSocket notification to admin
        notificacionService.notificarAdmin(
                "Nuevo trámite solicitado: " + referencia,
                "SISTEMA",
                tramite.getId(),
                "TRAMITE");

        return tramite;
    }

    public Tramite iniciarPorAdmin(String tramiteId) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Trámite", tramiteId));

        if (tramite.getEstado() != EstadoTramite.NUEVO) {
            throw new BusinessException("Solo se pueden iniciar trámites en estado NUEVO");
        }

        tramite.setEstado(EstadoTramite.EN_PROCESO);

        Politica politica = politicaRepository.findById(tramite.getPoliticaId())
                .orElseThrow(() -> new ResourceNotFoundException("Política", tramite.getPoliticaId()));

        String nodoId = tramite.getNodoActualId();
        Nodo nodo = getNodo(politica, nodoId);

        // Resolve funcionario name
        String nombreFuncionario = null;
        if (nodo.getResponsableId() != null) {
            nombreFuncionario = usuarioRepository.findById(nodo.getResponsableId())
                    .map(Usuario::getNombre).orElse(null);
        }

        Tarea tarea = Tarea.builder()
                .tramiteId(tramite.getId())
                .politicaId(politica.getId())
                .nodoId(nodoId)
                .nombreNodo(nodo.getNombre())
                .departamento(nodo.getDepartamento())
                .funcionarioId(nodo.getResponsableId())
                .nombreFuncionario(nombreFuncionario)
                .numeroReferenciaTramite(tramite.getNumeroReferencia())
                .nombrePolitica(politica.getNombre())
                .instrucciones(nodo.getDescripcion())
                .prioridad(tramite.getPrioridad())
                .fechaAsignacion(LocalDateTime.now())
                .build();

        tareaRepository.save(tarea);

        // Notify assigned funcionario via WebSocket
        if (nodo.getResponsableId() != null) {
            notificacionService.notificarFuncionario(
                    nodo.getResponsableId(),
                    "Nueva tarea asignada: " + nodo.getNombre() + " — Trámite " + tramite.getNumeroReferencia(),
                    "NUEVA_TAREA",
                    tarea.getId(),
                    "TAREA");
        }

        tramite.setFechaUltimaActualizacion(LocalDateTime.now());
        return tramiteRepository.save(tramite);
    }

    public Tramite save(Tramite tramite) {
        tramite.setFechaUltimaActualizacion(LocalDateTime.now());
        return tramiteRepository.save(tramite);
    }

    private String generarNumeroReferencia(LocalDateTime fecha) {
        int anio = fecha.getYear();
        long seq = sequenceService.nextVal(anio);
        return String.format("TRM-%d-%04d", anio, seq);
    }

    private Nodo getNodo(Politica politica, String nodoId) {
        return politica.getNodos().stream()
                .filter(n -> n.getId().equals(nodoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nodo '" + nodoId + "' no encontrado"));
    }
}
