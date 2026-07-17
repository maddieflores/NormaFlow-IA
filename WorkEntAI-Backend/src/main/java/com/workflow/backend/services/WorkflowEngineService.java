package com.workflow.backend.services;

import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.*;
import com.workflow.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor de workflow: orquesta el avance de un trámite a través de los nodos
 * definidos en la política. Aplica SRP — solo gestiona la lógica de flujo.
 *
 * Soporta:
 *  - Flujos lineales (TASK → TASK → END)
 *  - Flujos alternativos (DECISION con evaluador de expresiones — CU-05 Opción B)
 *  - Flujos paralelos con sincronización fork/JOIN (PARALLEL → ramas → JOIN — CU-13)
 *  - Flujos iterativos (ciclos con condición de salida)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngineService {

    private final TareaRepository tareaRepository;
    private final TramiteRepository tramiteRepository;
    private final PoliticaRepository politicaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    /** SRP: la evaluación de condiciones de DECISION es responsabilidad de este componente */
    private final CondicionEvaluador condicionEvaluador;

    // ── Completar tarea y avanzar el trámite ────────────────────────────────

    public void completarTarea(String tareaId, Map<String, Object> formularioDatos) {
        Tarea tarea = tareaRepository.findById(tareaId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarea", tareaId));

        if (tarea.getEstado() == EstadoTarea.COMPLETADO) {
            throw new BusinessException("La tarea ya fue completada anteriormente");
        }

        Tramite tramite = tramiteRepository.findById(tarea.getTramiteId())
                .orElseThrow(() -> new ResourceNotFoundException("Trámite", tarea.getTramiteId()));

        Politica politica = politicaRepository.findById(tramite.getPoliticaId())
                .orElseThrow(() -> new ResourceNotFoundException("Política", tramite.getPoliticaId()));

        Nodo nodoActual = getNodo(politica, tarea.getNodoId());

        // Validar campos requeridos del formulario antes de completar la tarea
        validarCamposRequeridos(nodoActual, formularioDatos);

        // Completar tarea y calcular duración
        LocalDateTime ahora = LocalDateTime.now();
        long duracion = Duration.between(tarea.getFechaAsignacion(), ahora).toMinutes();
        tarea.setEstado(EstadoTarea.COMPLETADO);
        tarea.setFormularioDatos(formularioDatos);
        tarea.setFechaCompletado(ahora);
        tarea.setDuracionMinutos(duracion);
        if (formularioDatos.containsKey("observacion")) {
            tarea.setObservacion(formularioDatos.get("observacion").toString());
        }
        tareaRepository.save(tarea);

        // Acumular datos del formulario en el trámite
        if (tramite.getDatosFormulario() == null) tramite.setDatosFormulario(new HashMap<>());
        tramite.getDatosFormulario().putAll(formularioDatos);

        // Registrar en historial
        agregarHistorial(tramite, nodoActual, tarea, formularioDatos, ahora, duracion);

        // Resolver siguiente nodo
        List<String> conexiones = nodoActual.getConexiones();
        if (conexiones == null || conexiones.isEmpty()) {
            finalizarTramite(tramite, ahora);
            return;
        }

        String siguienteNodoId = resolverSiguienteNodo(nodoActual, formularioDatos, tramite.getDatosFormulario(), conexiones);
        Nodo siguienteNodo = getNodo(politica, siguienteNodoId);

        // ── Nodo END ────────────────────────────────────────────────────────
        if ("END".equals(siguienteNodo.getTipo())) {
            finalizarTramite(tramite, ahora);
            return;
        }

        // ── Nodo JOIN (sincronización de ramos paralelos) ───────────────────
        if ("JOIN".equals(siguienteNodo.getTipo())) {
            boolean continuar = procesarJoin(tramite, siguienteNodo, ahora);
            if (!continuar) {
                // Aún hay ramas paralelas pendientes → solo guardar el trámite con el contador decrementado
                tramite.setFechaUltimaActualizacion(ahora);
                tramiteRepository.save(tramite);
                return;
            }
            // Todas las ramas completaron → avanzar al nodo siguiente del JOIN
            List<String> conexionesJoin = siguienteNodo.getConexiones();
            if (conexionesJoin == null || conexionesJoin.isEmpty()) {
                finalizarTramite(tramite, ahora);
                return;
            }
            String nodoPostJoinId = conexionesJoin.get(0);
            Nodo nodoPostJoin = getNodo(politica, nodoPostJoinId);
            if ("END".equals(nodoPostJoin.getTipo())) {
                finalizarTramite(tramite, ahora);
                return;
            }
            crearTarea(tramite, politica, nodoPostJoinId);
            tramite.setNodoActualId(nodoPostJoinId);
            tramite.setNombreNodoActual(nodoPostJoin.getNombre());
            tramite.setDepartamentoActual(nodoPostJoin.getDepartamento());
            tramite.setEstado(EstadoTramite.EN_PROCESO);
            tramite.setFechaUltimaActualizacion(ahora);
            tramiteRepository.save(tramite);
            return;
        }

        // ── Nodo PARALLEL (fork) ─────────────────────────────────────────────
        if ("PARALLEL".equals(siguienteNodo.getTipo())) {
            procesarParallelFork(tramite, politica, siguienteNodo, ahora);
            tramite.setNodoActualId(siguienteNodoId);
            tramite.setNombreNodoActual(siguienteNodo.getNombre());
            tramite.setDepartamentoActual(siguienteNodo.getDepartamento());
            tramite.setEstado(EstadoTramite.EN_PROCESO);
            tramite.setFechaUltimaActualizacion(ahora);
            tramiteRepository.save(tramite);
            return;
        }

        // ── Nodo TASK u otro tipo ────────────────────────────────────────────
        crearTarea(tramite, politica, siguienteNodoId);
        tramite.setNodoActualId(siguienteNodoId);
        tramite.setNombreNodoActual(siguienteNodo.getNombre());
        tramite.setDepartamentoActual(siguienteNodo.getDepartamento());
        tramite.setEstado(EstadoTramite.EN_PROCESO);
        tramite.setFechaUltimaActualizacion(ahora);
        tramiteRepository.save(tramite);
    }

    // ── Lógica PARALLEL fork ─────────────────────────────────────────────────

    /**
     * Procesa un nodo PARALLEL (fork): inicializa el contador de ramas para el JOIN
     * y crea tareas en todos los nodos de destino de las ramas paralelas.
     */
    private void procesarParallelFork(Tramite tramite, Politica politica, Nodo parallelNodo, LocalDateTime ahora) {
        List<String> ramas = parallelNodo.getConexiones();
        if (ramas == null || ramas.isEmpty()) {
            throw new BusinessException("El nodo PARALLEL '" + parallelNodo.getNombre() + "' no tiene ramas definidas");
        }

        String joinNodoId = parallelNodo.getJoinNodoId();
        if (joinNodoId != null && !joinNodoId.isBlank()) {
            // Inicializar o sobrescribir el contador para este JOIN
            if (tramite.getRamasParalelasActivas() == null) {
                tramite.setRamasParalelasActivas(new HashMap<>());
            }
            tramite.getRamasParalelasActivas().put(joinNodoId, ramas.size());
            log.info("Trámite {} — FORK: {} ramas paralelas, JOIN en nodo '{}'",
                    tramite.getNumeroReferencia(), ramas.size(), joinNodoId);
        } else {
            log.warn("Nodo PARALLEL '{}' no tiene joinNodoId definido. La sincronización JOIN no funcionará correctamente.",
                    parallelNodo.getNombre());
        }

        // Crear una tarea por cada rama paralela
        for (String ramaId : ramas) {
            crearTarea(tramite, politica, ramaId);
        }
    }

    // ── Lógica JOIN ──────────────────────────────────────────────────────────

    /**
     * Procesa la llegada de una rama al nodo JOIN.
     * Decrementa el contador de ramas pendientes.
     *
     * @return true si todas las ramas completaron y el flujo puede continuar; false si aún hay ramas pendientes.
     */
    private boolean procesarJoin(Tramite tramite, Nodo joinNodo, LocalDateTime ahora) {
        String joinNodoId = joinNodo.getId();

        if (tramite.getRamasParalelasActivas() == null) {
            tramite.setRamasParalelasActivas(new HashMap<>());
        }

        int restantes = tramite.getRamasParalelasActivas().getOrDefault(joinNodoId, 1);
        restantes--;

        log.info("Trámite {} — JOIN '{}': {} ramas restantes",
                tramite.getNumeroReferencia(), joinNodo.getNombre(), restantes);

        if (restantes <= 0) {
            // Todas las ramas completaron: limpiar el contador
            tramite.getRamasParalelasActivas().remove(joinNodoId);
            return true; // Continuar el flujo
        } else {
            // Aún hay ramas pendientes
            tramite.getRamasParalelasActivas().put(joinNodoId, restantes);
            return false; // Esperar
        }
    }

    // ── Resolución de siguiente nodo ─────────────────────────────────────────

    /**
     * Resuelve el ID del siguiente nodo a partir del tipo del nodo actual.
     *
     * Para DECISION: usa el evaluador de expresiones (CU-05, Opción B) si condicion está definida;
     * en caso contrario usa el campo legacy 'aprobado'.
     */
    private String resolverSiguienteNodo(Nodo nodo, Map<String, Object> datosRecien,
                                          Map<String, Object> datosAcumulados, List<String> conexiones) {
        if ("DECISION".equals(nodo.getTipo())) {
            CondicionNodo cond = nodo.getCondicion();
            if (cond != null && cond.getCampo() != null && !cond.getCampo().isBlank()
                    && cond.getNodoSiId() != null && cond.getNodoNoId() != null) {
                // Delegar al evaluador especializado (SRP + DIP)
                boolean resultado = condicionEvaluador.evaluar(cond, datosRecien, datosAcumulados);
                return resultado ? cond.getNodoSiId() : cond.getNodoNoId();
            }
            // Fallback legacy: campo 'aprobado' booleano
            Object aprobado = datosRecien.get("aprobado");
            boolean aprobadoVal = aprobado != null && Boolean.parseBoolean(aprobado.toString());
            return aprobadoVal ? conexiones.get(0)
                    : (conexiones.size() > 1 ? conexiones.get(1) : conexiones.get(0));
        }
        return conexiones.get(0);
    }

    // ── Crear tarea ──────────────────────────────────────────────────────────

    private void crearTarea(Tramite tramite, Politica politica, String nodoId) {
        Nodo nodo = getNodo(politica, nodoId);

        String nombreFuncionario = null;
        if (nodo.getResponsableId() != null) {
            nombreFuncionario = usuarioRepository.findById(nodo.getResponsableId())
                    .map(Usuario::getNombre).orElse(null);
        }

        Tarea nuevaTarea = Tarea.builder()
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
                .camposFormulario(nodo.getCamposFormulario() != null ? nodo.getCamposFormulario() : new ArrayList<>())
                .prioridad(tramite.getPrioridad())
                .fechaAsignacion(LocalDateTime.now())
                .build();

        tareaRepository.save(nuevaTarea);

        notificacionService.notificarFuncionario(
                nodo.getResponsableId(),
                "Nueva tarea asignada: " + nodo.getNombre() + " — Trámite " + tramite.getNumeroReferencia(),
                "NUEVA_TAREA",
                nuevaTarea.getId(),
                "TAREA"
        );
    }

    // ── Historial ────────────────────────────────────────────────────────────

    private void agregarHistorial(Tramite tramite, Nodo nodo, Tarea tarea,
                                   Map<String, Object> datos, LocalDateTime fecha, long duracion) {
        if (tramite.getHistorial() == null) tramite.setHistorial(new ArrayList<>());

        String resultado = null;
        if ("DECISION".equals(nodo.getTipo())) {
            CondicionNodo cond = nodo.getCondicion();
            if (cond != null && cond.getCampo() != null) {
                Object val = datos.get(cond.getCampo());
                resultado = val != null ? "Condición evaluada: " + cond.getCampo() + " → " + val : null;
            } else {
                Object aprobado = datos.get("aprobado");
                resultado = (aprobado != null && Boolean.parseBoolean(aprobado.toString()))
                        ? "APROBADO" : "RECHAZADO";
            }
        }

        HistorialTramite entrada = HistorialTramite.builder()
                .nodoId(nodo.getId())
                .nombreNodo(nodo.getNombre())
                .departamento(nodo.getDepartamento())
                .funcionarioId(tarea.getFuncionarioId())
                .nombreFuncionario(tarea.getNombreFuncionario())
                .accion("COMPLETADO")
                .observacion(tarea.getObservacion())
                .resultadoDecision(resultado)
                .duracionMinutos(duracion)
                .fecha(fecha)
                .build();

        tramite.getHistorial().add(entrada);
    }

    // ── Finalizar trámite ────────────────────────────────────────────────────

    private void finalizarTramite(Tramite tramite, LocalDateTime ahora) {
        long duracionTotal = Duration.between(tramite.getFechaInicio(), ahora).toMinutes();
        tramite.setEstado(EstadoTramite.COMPLETADO);
        tramite.setFechaFin(ahora);
        tramite.setFechaUltimaActualizacion(ahora);
        tramite.setDuracionMinutos(duracionTotal);
        tramiteRepository.save(tramite);

        notificacionService.notificarCliente(
                tramite.getClienteId(),
                "Tu trámite " + tramite.getNumeroReferencia() + " ha sido completado exitosamente.",
                "TRAMITE_COMPLETADO",
                tramite.getId(),
                "TRAMITE"
        );
    }

    // ── Validación de campos requeridos ──────────────────────────────────────

    private void validarCamposRequeridos(Nodo nodo, Map<String, Object> datos) {
        if (nodo.getCamposFormulario() == null) return;
        for (Map<String, Object> campo : nodo.getCamposFormulario()) {
            boolean requerido = Boolean.TRUE.equals(campo.get("requerido"));
            String nombre = (String) campo.get("nombre");
            if (requerido && (datos == null || !datos.containsKey(nombre)
                    || datos.get(nombre) == null
                    || datos.get(nombre).toString().isBlank())) {
                throw new BusinessException("Campo requerido faltante: " + nombre);
            }
        }
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private Nodo getNodo(Politica politica, String nodoId) {
        return politica.getNodos().stream()
                .filter(n -> n.getId().equals(nodoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Nodo '" + nodoId + "' no encontrado en la política"));
    }
}
