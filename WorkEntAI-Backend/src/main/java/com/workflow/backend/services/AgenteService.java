package com.workflow.backend.services;

import com.workflow.backend.ai.AIService;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.AgenteSession;
import com.workflow.backend.models.AgenteSession.MensajeDialogo;
import com.workflow.backend.models.Politica;
import com.workflow.backend.models.RequisitoTramite;
import com.workflow.backend.repositories.AgenteRepository;
import com.workflow.backend.repositories.PoliticaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio del Agente Inteligente de Atención al Cliente (Ciclo 2 — CU-22/23).
 *
 * Gestiona el ciclo de vida del diálogo en 3 fases:
 * IDENTIFICACION → identifica la política correcta para el cliente
 * REQUISITOS → guía al cliente por los requisitos iniciales
 * CONFIRMACION → confirma los datos antes de iniciar el trámite
 *
 * Principio SRP: solo gestiona el estado de la sesión;
 * la lógica de IA está en AIService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgenteService {

    private final AgenteRepository agenteRepository;
    private final PoliticaRepository politicaRepository;
    private final AIService aiService;
    private final TramiteService tramiteService;

    // ── Iniciar o retomar sesión ──────────────────────────────────────────────

    /**
     * Crea una nueva sesión de diálogo para un cliente.
     * Si ya existe una sesión activa, la devuelve para reanudar.
     */
    public AgenteSession iniciarSesion(String clienteId) {
        // Reutilizar sesión activa si existe
        return agenteRepository
                .findTopByClienteIdAndActivaTrueOrderByFechaCreacionDesc(clienteId)
                .orElseGet(() -> {
                    AgenteSession nueva = AgenteSession.builder()
                            .clienteId(clienteId)
                            .fase("IDENTIFICACION")
                            .activa(true)
                            .fechaCreacion(LocalDateTime.now())
                            .fechaActualizacion(LocalDateTime.now())
                            .build();
                    nueva = agenteRepository.save(nueva);
                    log.info("🤖 Nueva sesión de agente iniciada para cliente {}", clienteId);
                    return nueva;
                });
    }

    /**
     * Crea una nueva sesión de diálogo para un usuario no autenticado (Demo).
     */
    public AgenteSession iniciarSesionDemo() {
        AgenteSession nueva = AgenteSession.builder()
                .clienteId("demo")
                .fase("IDENTIFICACION")
                .activa(true)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        nueva = agenteRepository.save(nueva);
        log.info("🤖 Nueva sesión de agente DEMO iniciada: {}", nueva.getId());
        return nueva;
    }

    // ── Procesar mensaje del cliente ──────────────────────────────────────────

    /**
     * Procesa un mensaje del cliente y genera la respuesta del agente.
     * La lógica varía según la fase de la sesión.
     *
     * @param sessionId      ID de la sesión activa
     * @param mensajeCliente Mensaje enviado por el cliente
     * @return Sesión actualizada con la respuesta del agente añadida
     */
    public AgenteSession procesarMensaje(String sessionId, String mensajeCliente) {
        AgenteSession sesion = agenteRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada: " + sessionId));

        if (!sesion.isActiva()) {
            throw new IllegalStateException("La sesión ya está cerrada");
        }

        // Agregar mensaje del usuario al historial
        agregarMensaje(sesion, "USUARIO", mensajeCliente);

        // Procesar según fase
        String respuesta = switch (sesion.getFase()) {
            case "IDENTIFICACION" -> procesarFaseIdentificacion(sesion, mensajeCliente);
            case "REQUISITOS" -> procesarFaseRequisitos(sesion, mensajeCliente);
            case "CONFIRMACION" -> procesarFaseConfirmacion(sesion, mensajeCliente);
            default -> "Sesión en estado inesperado. Por favor inicia una nueva sesión.";
        };

        // Agregar respuesta del agente
        agregarMensaje(sesion, "AGENTE", respuesta);
        sesion.setFechaActualizacion(LocalDateTime.now());
        return agenteRepository.save(sesion);
    }

    // ── Fase 1: Identificación de política (CU-22) ────────────────────────────

    private String procesarFaseIdentificacion(AgenteSession sesion, String descripcionCliente) {
        // Obtener políticas activas disponibles
        List<Politica> politicasActivas = politicaRepository.findByEstado(
                com.workflow.backend.enums.EstadoPolitica.ACTIVA);

        if (politicasActivas.isEmpty()) {
            return "En este momento no hay políticas activas disponibles. Por favor contáctenos.";
        }

        // Usar IA para identificar la política más adecuada
        String respuestaIA = aiService.identificarPolitica(descripcionCliente, politicasActivas);

        // Parsear respuesta: esperamos JSON con politicaId y mensaje
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var json = mapper.readTree(respuestaIA);

            String politicaId = json.has("politicaId") ? json.get("politicaId").asText() : null;
            String mensajeAgente = json.has("mensaje") ? json.get("mensaje").asText() : respuestaIA;
            double confianza = json.has("confianza") ? json.get("confianza").asDouble() : 0.0;

            if (politicaId != null && !politicaId.isBlank() && confianza >= 0.6) {
                // Política identificada con suficiente confianza → avanzar a REQUISITOS
                Politica politica = politicaRepository.findById(politicaId).orElse(null);
                if (politica != null) {
                    sesion.setPoliticaId(politicaId);
                    sesion.setNombrePolitica(politica.getNombre());
                    sesion.setFase("REQUISITOS");
                    sesion.setRequisitoActualIndex(0);

                    String transicion = mensajeAgente + "\n\n✅ He identificado el trámite: **" +
                            politica.getNombre() + "**\n\nAhora te guiaré por los requisitos necesarios.";

                    // Si no hay requisitos, ir directo a confirmación
                    if (politica.getRequisitosIniciales() == null || politica.getRequisitosIniciales().isEmpty()) {
                        sesion.setFase("CONFIRMACION");
                        return transicion
                                + "\n\nEste trámite no requiere documentos previos. ¿Deseas iniciarlo ahora? (Sí/No)";
                    }

                    RequisitoTramite primerRequisito = politica.getRequisitosIniciales().get(0);
                    return transicion + "\n\n**Requisito 1:** " + primerRequisito.getNombre() +
                            "\n" + primerRequisito.getDescripcion() +
                            (primerRequisito.isObligatorio() ? " *(Obligatorio)*" : " *(Opcional)*") +
                            "\n\n¿Cuentas con este documento?";
                }
            }

            // No hay confianza suficiente → pedir más información
            return mensajeAgente;

        } catch (Exception e) {
            log.warn("⚠️ Error parseando respuesta IA en identificación: {}", e.getMessage());
            return respuestaIA;
        }
    }

    // ── Fase 2: Validación de requisitos (CU-23) ──────────────────────────────

    private String procesarFaseRequisitos(AgenteSession sesion, String respuestaCliente) {
        Politica politica = politicaRepository.findById(sesion.getPoliticaId())
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada"));

        List<RequisitoTramite> requisitos = politica.getRequisitosIniciales();
        int idx = sesion.getRequisitoActualIndex();

        // Registrar respuesta del cliente para el requisito actual
        if (idx < requisitos.size()) {
            RequisitoTramite requisito = requisitos.get(idx);
            boolean obligatorio = requisito.isObligatorio();
            boolean tieneArchivoFisico = respuestaCliente.contains("[Archivo adjunto:");
            boolean diceTener = respuestaCliente.toLowerCase().matches(".*(sí|si|s|tengo|cuento|disponible).*");

            if (obligatorio && !tieneArchivoFisico) {
                // Forzar a que suba el archivo físicamente
                return "⚠️ Este requisito es **obligatorio**. Por favor, adjunta el archivo correspondiente ("
                        + requisito.getNombre() + ") usando el icono de clip 📎 y envíalo para poder avanzar.";
            }

            if (tieneArchivoFisico || diceTener) {
                sesion.getDatosRecopilados().put("requisito_" + requisito.getNombre(), "CONFIRMADO");
            } else {
                sesion.getDatosRecopilados().put("requisito_" + requisito.getNombre(), "NO_DISPONIBLE");
            }

            sesion.setRequisitoActualIndex(idx + 1);
        }

        // Verificar si quedan más requisitos
        int siguienteIdx = sesion.getRequisitoActualIndex();
        if (siguienteIdx < requisitos.size()) {
            RequisitoTramite siguiente = requisitos.get(siguienteIdx);
            return "✅ Anotado.\n\n**Requisito " + (siguienteIdx + 1) + ":** " +
                    siguiente.getNombre() + "\n" + siguiente.getDescripcion() +
                    (siguiente.isObligatorio() ? " *(Obligatorio)*" : " *(Opcional)*") +
                    "\n\n¿Cuentas con este documento?";
        }

        // Todos los requisitos procesados → avanzar a confirmación
        sesion.setFase("CONFIRMACION");
        long confirmados = sesion.getDatosRecopilados().values().stream()
                .filter(v -> v.equals("CONFIRMADO")).count();
        return "✅ Hemos revisado todos los " + requisitos.size() + " requisito(s).\n" +
                "Tienes **" + confirmados + "** confirmado(s).\n\n" +
                "¿Deseas iniciar el trámite **" + sesion.getNombrePolitica() + "** ahora? (Sí/No)";
    }

    // ── Fase 3: Confirmación ──────────────────────────────────────────────────

    private String procesarFaseConfirmacion(AgenteSession sesion, String respuestaCliente) {
        boolean confirma = respuestaCliente.toLowerCase().matches(".*(sí|si|s|confirmo|iniciar|empezar).*");

        if (confirma) {
            sesion.setFase("COMPLETADA");
            sesion.setActiva(false);

            // Si es una sesión demo, no creamos un trámite real en la base de datos
            if ("demo".equals(sesion.getClienteId())) {
                return "🎉 ¡Excelente! En un entorno real con cuenta, tu trámite habría sido creado exitosamente en este paso.\n\n"
                        +
                        "Para iniciar este trámite y realizar su seguimiento, por favor **inicia sesión** o **regístrate** en la plataforma. ¡Te esperamos! 😊";
            }

            // Crear el trámite automáticamente
            com.workflow.backend.models.Tramite nuevoTramite = tramiteService.iniciarTramite(
                    sesion.getPoliticaId(),
                    sesion.getClienteId(),
                    "Iniciado vía Agente Inteligente (Asistente IA)");

            sesion.setTramiteId(nuevoTramite.getId());

            return "🎉 ¡Perfecto! Tu trámite ha sido creado exitosamente.\n\n" +
                    "El código de seguimiento de tu trámite es: **" + nuevoTramite.getNumeroReferencia() + "**.\n" +
                    "Puedes consultarlo en cualquier momento desde tu panel principal.";
        } else {
            sesion.setFase("ABANDONADA");
            sesion.setActiva(false);
            return "Entendido. Si cambias de opinión, puedes iniciar el proceso nuevamente. " +
                    "¡Estamos aquí para ayudarte! 😊";
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void agregarMensaje(AgenteSession sesion, String rol, String contenido) {
        sesion.getMensajes().add(MensajeDialogo.builder()
                .rol(rol)
                .contenido(contenido)
                .timestamp(LocalDateTime.now())
                .build());
    }

    public AgenteSession obtenerSesion(String sessionId) {
        return agenteRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión no encontrada: " + sessionId));
    }

    public void cerrarSesion(String sessionId) {
        agenteRepository.findById(sessionId).ifPresent(s -> {
            s.setActiva(false);
            s.setFase("ABANDONADA");
            agenteRepository.save(s);
        });
    }

    /**
     * Reclama una sesión de demo asociándola al cliente logueado y creando el
     * trámite real.
     */
    public AgenteSession reclamarSesion(String sessionId, String clienteId) {
        AgenteSession sesion = agenteRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sesión demo no encontrada: " + sessionId));

        if (!"demo".equals(sesion.getClienteId())) {
            throw new IllegalStateException("Esta sesión ya está asociada a un cliente.");
        }

        // Asociar al cliente real
        sesion.setClienteId(clienteId);

        // Si ya identificamos la política, forzar fase de completado y crear trámite
        // real
        if (sesion.getPoliticaId() != null) {
            sesion.setFase("COMPLETADA");
            sesion.setActiva(false);

            com.workflow.backend.models.Tramite nuevoTramite = tramiteService.iniciarTramite(
                    sesion.getPoliticaId(),
                    clienteId,
                    "Iniciado vía Agente Inteligente (Demo Reclamada)");

            sesion.setTramiteId(nuevoTramite.getId());
        } else {
            // Si por alguna razón no se identificó la política, la dejamos activa para el
            // cliente
            sesion.setFase("IDENTIFICACION");
        }

        return agenteRepository.save(sesion);
    }
}
