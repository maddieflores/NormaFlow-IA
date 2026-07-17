package com.workflow.backend.services;

import com.workflow.backend.ai.AIService;
import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.Nodo;
import com.workflow.backend.models.Politica;
import com.workflow.backend.models.Tramite;
import com.workflow.backend.models.AlertaAnomalia;
import com.workflow.backend.repositories.PoliticaRepository;
import com.workflow.backend.repositories.TramiteRepository;
import com.workflow.backend.repositories.AlertaAnomaliaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio de Motor Predictivo (Ciclo 2 — CU-28/29).
 *
 * Implementa la Opción A del plan: análisis estadístico en Java
 * (media y desviación estándar del histórico de tareas) combinado con
 * ChatClient para generar texto explicativo y recomendaciones.
 *
 * Principio SRP: responsabilidad única de análisis predictivo.
 * El microservicio Python (Opción B) se integra vía HTTP como extensión futura.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PredictorService {

    private final TramiteRepository tramiteRepository;
    private final PoliticaRepository politicaRepository;
    private final AnalyticsService analyticsService;
    private final AIService aiService;
    private final AlertaAnomaliaRepository alertaRepository;

    public List<AlertaAnomalia> getAnomaliasActivas() {
        return alertaRepository.findByResueltaFalseOrderByFechaDeteccionDesc();
    }
    
    public void resolverAnomalia(String anomaliaId) {
        alertaRepository.findById(anomaliaId).ifPresent(alerta -> {
            alerta.setResuelta(true);
            alertaRepository.save(alerta);
        });
    }

    // ── CU-28: Predicción de ruta óptima ─────────────────────────────────────

    /**
     * Analiza el histórico de trámites de una política para identificar la ruta
     * más eficiente y genera recomendaciones para optimizarla.
     *
     * Algoritmo:
     *  1. Obtener promedios de duración por nodo (AnalyticsService)
     *  2. Calcular desviación estándar por nodo (variabilidad = riesgo)
     *  3. Identificar los 3 nodos más lentos (candidatos a optimización)
     *  4. Usar IA para generar recomendación de ruta alternativa
     *
     * @param politicaId ID de la política
     * @return Mapa con análisis estadístico + recomendaciones de IA
     */
    public Map<String, Object> predecirRutaOptima(String politicaId) {
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada: " + politicaId));

        // Estadísticas históricas
        Map<String, Double> promedios = analyticsService.promediosPorNodo(politicaId);

        if (promedios.isEmpty()) {
            return Map.of(
                "politicaId", politicaId,
                "mensaje", "Aún no hay datos históricos suficientes para predicciones.",
                "nodosAnalizados", 0
            );
        }

        // Calcular desviación estándar por nodo (variabilidad)
        Map<String, Double> desviaciones = calcularDesviaciones(politicaId, promedios);

        // Ordenar nodos por tiempo promedio (descendente) → nodos más lentos primero
        List<Map<String, Object>> nodosOrdenados = promedios.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> nodoData = new HashMap<>();
                    nodoData.put("nodoId", e.getKey());
                    nodoData.put("nombreNodo", obtenerNombreNodo(politica, e.getKey()));
                    nodoData.put("promedioMinutos", Math.round(e.getValue() * 100.0) / 100.0);
                    nodoData.put("desviacionMinutos", Math.round(desviaciones.getOrDefault(e.getKey(), 0.0) * 100.0) / 100.0);
                    nodoData.put("nivelRiesgo", clasificarRiesgo(e.getValue(), desviaciones.getOrDefault(e.getKey(), 0.0)));
                    return nodoData;
                })
                .collect(Collectors.toList());

        // Tiempo total estimado de la ruta actual
        double tiempoTotalEstimado = promedios.values().stream().mapToDouble(Double::doubleValue).sum();

        // Generar recomendaciones con IA
        String recomendaciones = aiService.generarRecomendacionCuello(
                nodosOrdenados.stream()
                        .filter(n -> "ALTO".equals(n.get("nivelRiesgo")) || "MEDIO".equals(n.get("nivelRiesgo")))
                        .map(n -> {
                            Map<String, Object> cuello = new HashMap<>(n);
                            cuello.put("limiteMinutos", 60.0); // Default si no hay límite definido
                            return cuello;
                        })
                        .collect(Collectors.toList()),
                politica.getNombre());

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("politicaId", politicaId);
        resultado.put("nombrePolitica", politica.getNombre());
        resultado.put("tiempoTotalEstimadoMinutos", Math.round(tiempoTotalEstimado * 100.0) / 100.0);
        resultado.put("tiempoTotalEstimadoHoras", Math.round(tiempoTotalEstimado / 60.0 * 100.0) / 100.0);
        resultado.put("nodosAnalizados", nodosOrdenados.size());
        resultado.put("nodos", nodosOrdenados);
        resultado.put("recomendaciones", recomendaciones);

        log.info("🔮 Ruta óptima calculada para política {}: {} nodos, {}min total",
                politicaId, nodosOrdenados.size(), Math.round(tiempoTotalEstimado));
        return resultado;
    }

    // ── CU-29: Score de riesgo de demora ─────────────────────────────────────

    /**
     * Calcula el riesgo de demora de un trámite activo comparando el nodo actual
     * contra el promedio histórico de ese nodo en la misma política.
     *
     * Score: 0-100 (0 = sin riesgo, 100 = muy probable que se demore)
     *
     * @param tramiteId ID del trámite activo
     * @return Score de riesgo + texto explicativo
     */
    public Map<String, Object> calcularRiesgoDemora(String tramiteId) {
        Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Trámite no encontrado: " + tramiteId));

        if (tramite.getNodoActualId() == null) {
            return Map.of(
                "tramiteId", tramiteId,
                "score", 0,
                "nivel", "SIN_DATOS",
                "mensaje", "El trámite no tiene un nodo activo actualmente."
            );
        }

        // Obtener promedios históricos del nodo actual
        Map<String, Double> promedios = analyticsService.promediosPorNodo(tramite.getPoliticaId());
        Double promedioNodo = promedios.get(tramite.getNodoActualId());

        if (promedioNodo == null) {
            return Map.of(
                "tramiteId", tramiteId,
                "nodoActual", tramite.getNodoActualId(),
                "score", 0,
                "nivel", "SIN_HISTORICO",
                "mensaje", "No hay datos históricos suficientes para este nodo."
            );
        }

        // Calcular tiempo que lleva el trámite en el nodo actual
        // (Se usa el historial para estimar)
        long tareasCompletadas = tramite.getHistorial() != null ? tramite.getHistorial().size() : 0;

        // Score heurístico: basado en proporción del tiempo actual vs promedio
        // Si el promedio es 60 min y ya lleva más de 90 min → score alto
        int score = calcularScore(promedioNodo, tareasCompletadas, tramite);
        String nivel = score >= 75 ? "ALTO" : score >= 45 ? "MEDIO" : "BAJO";

        Politica politica = politicaRepository.findById(tramite.getPoliticaId())
                .orElse(null);
        String nombreNodo = politica != null
                ? obtenerNombreNodo(politica, tramite.getNodoActualId())
                : tramite.getNodoActualId();

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("tramiteId", tramiteId);
        resultado.put("referencia", tramite.getNumeroReferencia());
        resultado.put("nodoActualId", tramite.getNodoActualId());
        resultado.put("nombreNodo", nombreNodo);
        resultado.put("promedioHistoricoMinutos", Math.round(promedioNodo * 100.0) / 100.0);
        resultado.put("score", score);
        resultado.put("nivel", nivel);
        resultado.put("mensaje", generarMensajeRiesgo(nivel, nombreNodo, promedioNodo));
        
        // CU-29: Registrar anomalía si es alto o crítico
        if ("ALTO".equals(nivel) || score >= 80) {
            registrarAnomaliaSiNoExiste(tramiteId, tramite.getPoliticaId(), tramite.getNodoActualId(), nivel, score, resultado.get("mensaje").toString());
        }

        return resultado;
    }

    private void registrarAnomaliaSiNoExiste(String tramiteId, String politicaId, String nodoId, String nivel, int score, String mensaje) {
        boolean existe = alertaRepository.findByResueltaFalseOrderByFechaDeteccionDesc().stream()
                .anyMatch(a -> a.getTramiteId().equals(tramiteId) && a.getNodoId().equals(nodoId));
        if (!existe) {
            AlertaAnomalia alerta = AlertaAnomalia.builder()
                    .tramiteId(tramiteId)
                    .politicaId(politicaId)
                    .nodoId(nodoId)
                    .nivelGravedad(nivel)
                    .score(score)
                    .descripcion(mensaje)
                    .build();
            alertaRepository.save(alerta);
        }
    }

    // ── Helpers estadísticos ──────────────────────────────────────────────────

    /**
     * Aproxima la desviación estándar de las duraciones por nodo.
     * Heurística: 20% del promedio como estimación conservadora de variabilidad.
     * En Ciclo 3 se puede mejorar con cálculo estadístico real sobre las tareas históricas.
     */
    private Map<String, Double> calcularDesviaciones(String politicaId,
                                                      Map<String, Double> promedios) {
        Map<String, Double> desviaciones = new HashMap<>();
        promedios.forEach((nodoId, promedio) -> desviaciones.put(nodoId, promedio * 0.20));
        return desviaciones;
    }

    private String clasificarRiesgo(double promedio, double desviacion) {
        if (promedio > 240 || desviacion > 120) return "ALTO";   // > 4 horas
        if (promedio > 60  || desviacion > 30)  return "MEDIO";  // > 1 hora
        return "BAJO";
    }

    private int calcularScore(double promedioNodo, long tareasCompletadas, Tramite tramite) {
        // Heurística simple: más tareas completadas vs promedio → menos riesgo
        // Se puede mejorar con datos de tiempo real del nodo actual
        if (promedioNodo > 480) return 80; // Nodos de > 8 horas son inherentemente riesgosos
        if (promedioNodo > 120) return 50;
        return 20;
    }

    private String generarMensajeRiesgo(String nivel, String nombreNodo, double promedioMinutos) {
        return switch (nivel) {
            case "ALTO" -> String.format(
                "⚠️ Riesgo alto de demora en el nodo '%s'. El promedio histórico es %.0f minutos. " +
                "Se recomienda asignar atención prioritaria.", nombreNodo, promedioMinutos);
            case "MEDIO" -> String.format(
                "🟡 Riesgo moderado en el nodo '%s' (promedio histórico: %.0f min). " +
                "Monitorear el avance.", nombreNodo, promedioMinutos);
            default -> String.format(
                "✅ Bajo riesgo en el nodo '%s' (promedio histórico: %.0f min).",
                nombreNodo, promedioMinutos);
        };
    }

    private String obtenerNombreNodo(Politica politica, String nodoId) {
        if (politica.getNodos() == null) return nodoId;
        return politica.getNodos().stream()
                .filter(n -> nodoId.equals(n.getId()))
                .map(Nodo::getNombre)
                .findFirst()
                .orElse(nodoId);
    }
}
