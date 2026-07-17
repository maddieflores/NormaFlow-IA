package com.workflow.backend.ai;

import com.workflow.backend.models.Nodo;
import com.workflow.backend.models.Politica;
import com.workflow.backend.repositories.PoliticaRepository;
import com.workflow.backend.services.AnalyticsService;
import com.workflow.backend.services.NotificacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class AIService {

    @Value("${ai.service.url:http://localhost:8000}")
    private String aiServiceUrl;

    private final PoliticaRepository politicaRepository;
    private final AnalyticsService analyticsService;
    private final NotificacionService notificacionService;
    private final org.springframework.ai.chat.client.ChatClient groqChatClient;
    private final org.springframework.ai.chat.client.ChatClient ollamaChatClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public AIService(PoliticaRepository politicaRepository,
            AnalyticsService analyticsService,
            NotificacionService notificacionService,
            @org.springframework.beans.factory.annotation.Qualifier("openAiChatModel") org.springframework.ai.chat.model.ChatModel groqModel,
            @org.springframework.beans.factory.annotation.Qualifier("ollamaChatModel") org.springframework.ai.chat.model.ChatModel ollamaModel) {
        this.politicaRepository = politicaRepository;
        this.analyticsService = analyticsService;
        this.notificacionService = notificacionService;

        // IA en la Nube (Rápida y potente)
        this.groqChatClient = org.springframework.ai.chat.client.ChatClient.builder(groqModel).build();

        // IA 100% Local (Privada y sin internet)
        this.ollamaChatClient = org.springframework.ai.chat.client.ChatClient.builder(ollamaModel).build();
    }

    private String executePromptConFallback(String systemPrompt, String userPrompt) {
        try {
            log.info("Llamando a Groq (Spring AI)...");
            return groqChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Fallo al comunicarse con Spring AI (Groq): {}", e.getMessage());
            throw new RuntimeException("Servicio de IA no disponible: " + e.getMessage());
        }
    }

    private String executeLocalPrompt(String systemPrompt, String userPrompt) {
        try {
            log.info("Llamando a Ollama Local (Spring AI)...");
            return ollamaChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("Fallo al comunicarse con Ollama Local: {}", e.getMessage());
            throw new RuntimeException("Servicio de IA Local no disponible. ¿Está Ollama encendido? " + e.getMessage());
        }
    }

    // ── 1. Copilot de diagramas ──────────────────────────────────
    public String procesarPromptDiagrama(String prompt) {
        String systemPrompt = """
                Eres un asistente experto en diseño de diagramas de actividades UML 2.5 para sistemas de workflow.

                Cuando el usuario pida agregar, conectar o modificar nodos, responde ÚNICAMENTE con un JSON válido
                con esta estructura exacta (sin texto adicional, sin markdown, solo el JSON):

                {
                  "accion": "AGREGAR_NODO" | "CONECTAR_NODOS" | "ELIMINAR_NODO" | "AGREGAR_MULTIPLES" | "DESCRIPCION",
                  "nodos": [
                    {
                      "key": "id_unico_numerico",
                      "text": "Nombre del nodo",
                      "category": "Start" | "End" | "" | "Decision" | "Parallel",
                      "departamento": "Nombre del departamento (opcional)"
                    }
                  ],
                  "links": [
                    { "from": "id_origen", "to": "id_destino", "text": "etiqueta opcional" }
                  ],
                  "mensaje": "Explicación breve en español de lo que hiciste"
                }

                Reglas importantes:
                - category "" = nodo TASK (tarea normal)
                - category "Decision" = nodo de decisión (rombo)
                - category "Start" = inicio del flujo
                - category "End" = fin del flujo
                - category "Parallel" = ejecución paralela
                - Los IDs deben ser números únicos (ej: 100, 101, 102)
                - Si el usuario pide conectar nodos existentes, usa solo "links" sin "nodos"
                - Si no entiendes, usa accion "DESCRIPCION" y explica en "mensaje"

                Ejemplos:
                Usuario: "Agrega un nodo de revisión legal después del técnico"
                Respuesta: {"accion":"AGREGAR_NODO","nodos":[{"key":"100","text":"Revisión Legal","category":"","departamento":"Dept. Legal"}],"links":[],"mensaje":"Nodo de Revisión Legal agregado. Conéctalo manualmente al nodo técnico."}

                Usuario: "Agrega un flujo completo: inicio, verificación, aprobación, fin"
                Respuesta: {"accion":"AGREGAR_MULTIPLES","nodos":[{"key":"200","text":"Inicio","category":"Start"},{"key":"201","text":"Verificación","category":""},{"key":"202","text":"¿Aprobado?","category":"Decision"},{"key":"203","text":"Fin","category":"End"}],"links":[{"from":"200","to":"201"},{"from":"201","to":"202"},{"from":"202","to":"203","text":"Sí"}],"mensaje":"Flujo completo creado con 4 nodos."}
                """;
        return executePromptConFallback(systemPrompt, prompt);
    }

    // ── 2. Extracción de datos de documento ──────────────────────
    public String extraerDatosDocumento(String textoDocumento, String nombreNodo) {
        String systemPrompt = """
                Eres un asistente que extrae datos clave de documentos para formularios de workflow.
                Analiza el texto del documento y extrae la información relevante para el nodo: %s
                Responde SOLO con un JSON con los campos extraídos. Ejemplo:
                {
                  "observacion": "texto extraído",
                  "aprobado": "true o false según el contenido",
                  "datos_adicionales": "cualquier dato relevante encontrado"
                }
                """.formatted(nombreNodo);
        return executePromptConFallback(systemPrompt, "Documento a analizar:\n" + textoDocumento);
    }

    // ── 3. Detección de cuellos de botella ───────────────────────
    public Map<String, Object> detectarCuellosBottella(String politicaId) {
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new com.workflow.backend.exception.ResourceNotFoundException(
                        "Política no encontrada: " + politicaId));

        Map<String, Double> promediosPorNodo = analyticsService.promediosPorNodo(politicaId);

        List<Map<String, Object>> cuellos = new ArrayList<>();

        for (Nodo nodo : politica.getNodos()) {
            if (nodo.getTiempoLimiteHoras() == null)
                continue;

            Double avg = promediosPorNodo.get(nodo.getId());
            if (avg == null)
                continue;

            int diasEspera = (int) (avg / 1440); // convertir min a días

            try {
                String url = aiServiceUrl + "/api/v1/anomalies/detect";
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> reqBody = new HashMap<>();
                reqBody.put("tramite_id", politicaId);
                reqBody.put("paso_actual", nodo.getNombre());
                reqBody.put("tiempo_espera_dias", diasEspera);
                reqBody.put("departamento", nodo.getDepartamento() != null ? nodo.getDepartamento() : "General");

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(reqBody, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Boolean isAnomaly = (Boolean) response.getBody().get("is_anomaly");
                    Double conf = (Double) response.getBody().get("confidence");

                    if (Boolean.TRUE.equals(isAnomaly)) {
                        notificacionService.notificarAdmin(
                                "Cuello de botella detectado por IA (TensorFlow) en nodo: " + nodo.getNombre() +
                                        " (Confianza: " + Math.round(conf * 100) + "%)",
                                "CUELLO_BOTELLA",
                                politicaId,
                                "TRAMITE");

                        Map<String, Object> cuello = new HashMap<>();
                        cuello.put("nodoId", nodo.getId());
                        cuello.put("nombreNodo", nodo.getNombre());
                        cuello.put("promedioMinutos", avg);
                        cuello.put("confianza", conf);

                        double limiteMinutos = nodo.getTiempoLimiteHoras() * 60.0;
                        double excesoPct = 0.0;
                        if (limiteMinutos > 0 && avg > limiteMinutos) {
                            excesoPct = ((avg - limiteMinutos) / limiteMinutos) * 100.0;
                        }
                        cuello.put("excesoPct", excesoPct);
                        cuello.put("excesoPorcentaje", excesoPct);

                        cuellos.add(cuello);
                    }
                }
            } catch (Exception e) {
                log.error("Fallo al detectar anomalía con FastAPI: {}", e.getMessage());
            }
        }

        // Fetch metrics for total and average
        long totalTareas = analyticsService.getCountTareasCompletadasPorPolitica(politicaId);
        double promedioMinutos = analyticsService.getPromedioDuracionPorPolitica(politicaId);

        // Generate dynamic recommendation
        String recomendacion = "";
        if (totalTareas == 0) {
            recomendacion = "No se registran trámites completados para esta política todavía, por lo que no es posible calcular cuellos de botella.";
        } else if (!cuellos.isEmpty()) {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("La política '").append(politica.getNombre())
                    .append("' presenta los siguientes cuellos de botella en sus nodos:\n");
            for (Map<String, Object> c : cuellos) {
                promptBuilder.append("- Nodo: ").append(c.get("nombreNodo"))
                        .append(", promedio de ejecución: ").append(Math.round((Double) c.get("promedioMinutos")))
                        .append(" minutos.\n");
            }
            promptBuilder.append(
                    "\nGenera una recomendación de 2 o 3 oraciones en español aconsejando cómo optimizar este flujo o reasignar recursos.");

            try {
                recomendacion = executePromptConFallback("Eres un analista de procesos experto.",
                        promptBuilder.toString());
            } catch (Exception e) {
                recomendacion = "Se recomienda revisar los tiempos asignados a las tareas señaladas y capacitar al personal de los departamentos correspondientes.";
            }
        } else {
            recomendacion = "No se detectaron demoras ni cuellos de botella significativos en esta política. El flujo se ejecuta de manera eficiente.";
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("politicaId", politicaId);
        resultado.put("promediosPorNodo", promediosPorNodo);
        resultado.put("cuellosDetectados", cuellos);
        resultado.put("cuellosBottella", cuellos); // Dual compatible
        resultado.put("totalTareas", totalTareas);
        resultado.put("promedioMinutos", promedioMinutos);
        resultado.put("recomendacion", recomendacion);
        resultado.put("fechaAnalisis", LocalDateTime.now().toString());
        return resultado;
    }

    // ── 4. Generación de diagrama UML PlantUML ───────────────────
    public String generarPlantUML(String politicaId) {
        Politica politica = politicaRepository.findById(politicaId)
                .orElseThrow(() -> new com.workflow.backend.exception.ResourceNotFoundException(
                        "Política no encontrada: " + politicaId));

        StringBuilder descripcion = new StringBuilder();
        descripcion.append("Política: ").append(politica.getNombre()).append("\n");
        descripcion.append("Nodos:\n");
        for (Nodo nodo : politica.getNodos()) {
            descripcion.append("- ID: ").append(nodo.getId())
                    .append(", Nombre: ").append(nodo.getNombre())
                    .append(", Tipo: ").append(nodo.getTipo())
                    .append(", Departamento: ").append(nodo.getDepartamento())
                    .append(", Conexiones: ").append(nodo.getConexiones())
                    .append("\n");
        }

        String systemPrompt = """
                Eres un experto en UML 2.5. Genera código PlantUML de diagrama de actividades
                para el workflow descrito. Usa la sintaxis PlantUML estándar:
                - @startuml / @enduml
                - start / stop para inicio y fin
                - :Nombre Actividad; para tareas (TASK)
                - if (¿Condición?) then (Sí) / else (No) / endif para decisiones (DECISION)
                - fork / fork again / end fork para paralelos (PARALLEL)
                - Agrega swimlanes con |Departamento| cuando el departamento esté disponible
                - Responde ÚNICAMENTE con el código PlantUML, sin explicaciones ni markdown
                """;

        return executePromptConFallback(systemPrompt, descripcion.toString());
    }

    // ── 5. Asistente de usuario (chatbot) ─────────────────────────
    public String asistente(String pregunta) {
        String systemPrompt = """
                Eres WorkBot, el asistente inteligente de WorkEntAI, un sistema de gestión de
                workflows para empresas. Ayudas a los usuarios a entender cómo usar el sistema.
                El sistema tiene 3 roles: ADMIN (crea políticas), FUNCIONARIO (atiende tareas),
                CLIENTE (consulta trámites). Responde en español, de forma amigable y concisa.
                """;
        // Aquí usamos la IA Local de Ollama en lugar de Groq
        return executeLocalPrompt(systemPrompt, pregunta);
    }

    // ── 6. Identificación de política para el Agente (CU-22) con Sentence-BERT ──
    public String identificarPolitica(String descripcionCliente,
            java.util.List<com.workflow.backend.models.Politica> politicasActivas) {

        StringBuilder listaPoliticas = new StringBuilder();
        for (Politica p : politicasActivas) {
            listaPoliticas.append("- ID: ").append(p.getId())
                    .append(", Nombre: ").append(p.getNombre())
                    .append(", Descripcion: ").append(p.getDescripcion() != null ? p.getDescripcion() : "");

            if (p.getRequisitosIniciales() != null && !p.getRequisitosIniciales().isEmpty()) {
                listaPoliticas.append(", Requisitos: ");
                p.getRequisitosIniciales().forEach(r -> {
                    listaPoliticas.append(r.getNombre()).append(r.isObligatorio() ? " (Obligatorio) " : " (Opcional) ");
                });
            }
            listaPoliticas.append("\n");
        }

        String systemPrompt = """
                Eres el asistente de identificación de trámites de WorkEntAI.
                Analiza la descripción del problema del cliente y selecciona la política más adecuada de la lista proporcionada.

                Lista de políticas activas:
                %s

                Responde ÚNICAMENTE con este JSON (sin markdown, sin texto extra):
                {
                  "politicaId": "id_de_la_politica_seleccionada_o_null_si_no_aplica",
                  "nombrePolitica": "nombre de la política",
                  "confianza": 0.95,
                  "mensaje": "Explicación amigable al cliente de por qué esta política aplica, y si tiene requisitos obligatorios, pídele explícitamente que los prepare o adjunte indicando que se permiten archivos PDF, imágenes (JPG, PNG), Word, Excel, audio y video."
                }

                Si la descripción del cliente no se relaciona de manera obvia con ninguna política de la lista, usa politicaId: null y explica
                en el mensaje qué información adicional necesitas.
                """
                .formatted(listaPoliticas.toString());

        String contexto = "Consulta original del cliente: " + descripcionCliente;

        return executeLocalPrompt(systemPrompt, contexto);
    }

    // ── 7. Orientación sobre requisito (CU-23) ────────────────────────────────
    public String guiarRequisito(com.workflow.backend.models.RequisitoTramite requisito,
            String nombrePolitica) {
        String systemPrompt = """
                Eres un asistente de orientación ciudadana de WorkEntAI. Un cliente está iniciando
                un trámite y no cuenta con un requisito obligatorio. Tu tarea es explicarle de forma
                clara y empática cómo puede obtener ese documento o qué alternativas existen.
                Responde en 2-3 párrafos cortos, en español, de forma amigable.
                """;
        String user = "Trámite: " + nombrePolitica + "\n" +
                "Requisito faltante: " + requisito.getNombre() + "\n" +
                "Descripción del requisito: " + requisito.getDescripcion();
        return executeLocalPrompt(systemPrompt, user);
    }

    // ── 8. Recomendaciones de optimización de cuellos (RF-062) ───────────────
    public String generarRecomendacionCuello(
            java.util.List<java.util.Map<String, Object>> cuellosDetectados,
            String nombrePolitica) {

        if (cuellosDetectados.isEmpty()) {
            return "✅ No se detectaron cuellos de botella en esta política.";
        }

        StringBuilder resumen = new StringBuilder();
        for (var cuello : cuellosDetectados) {
            resumen.append("- Nodo: ").append(cuello.get("nombreNodo"))
                    .append(" | Promedio: ").append(cuello.get("promedioMinutos")).append(" min")
                    .append(" | Límite: ").append(cuello.get("limiteMinutos")).append(" min")
                    .append(" | Exceso: ").append(
                            String.format("%.1f",
                                    (Double) cuello.get("promedioMinutos") - (Double) cuello.get("limiteMinutos")))
                    .append(" min\n");
        }

        String systemPrompt = """
                Eres un experto en optimización de procesos de negocio y workflows.
                Analiza los cuellos de botella detectados en el workflow y genera recomendaciones
                específicas y accionables para cada nodo problemático.

                Para cada cuello, sugiere: (1) causas probables, (2) acciones concretas para reducir
                el tiempo, (3) métricas de seguimiento sugeridas.

                Responde en formato estructurado con encabezados por nodo, en español.
                """;

        return executePromptConFallback(systemPrompt,
                "Política: " + nombrePolitica + "\n\nCuellos detectados:\n" + resumen);
    }

    // ── 9. Generación de reportes por lenguaje natural (CU-30) ───────────────
    public String generarReporte(String solicitudNatural,
            java.util.Map<String, Object> datosAnalytics) {
        String systemPrompt = """
                Eres un generador de reportes de negocio para WorkEntAI.
                Recibirás una solicitud en lenguaje natural y datos de analytics del sistema.
                Tu tarea es generar un reporte estructurado en JSON con los datos relevantes.

                Responde ÚNICAMENTE con este JSON (sin markdown, sin texto extra):
                {
                  "titulo": "Título descriptivo del reporte",
                  "columnas": ["Columna 1", "Columna 2", ...],
                  "filas": [
                    ["valor1", "valor2", ...],
                    ...
                  ],
                  "resumen": "Resumen ejecutivo del reporte en 2-3 oraciones.",
                  "exportable": true
                }

                Usa SOLO los datos proporcionados. No inventes datos.
                Si los datos no son suficientes para el reporte solicitado, devuelve
                un reporte vacío con un resumen explicativo.
                """;

        String contexto = "Solicitud: " + solicitudNatural + "\n\nDatos disponibles:\n" +
                datosAnalytics.toString();

        return executePromptConFallback(systemPrompt, contexto);
    }

    public String analizarSolicitudReporte(String prompt) {
        String systemPrompt = """
                Eres un asistente analista de datos para un sistema de Workflows.
                El usuario te pedirá un reporte en lenguaje natural. Debes extraer su intención y mapearla
                a uno de los siguientes tipos predefinidos:
                1. "politica_mas_utilizada"
                2. "cliente_mas_tramites"
                3. "tiempo_promedio_depto"
                4. "eficiencia_funcionarios"
                5. "tramites_riesgo_demora"
                6. "documentos_generados"

                Si no encaja en ninguno o falta información clave, devuelve tipo "desconocido" y un mensaje conversacional pidiendo aclaración.

                Responde ÚNICAMENTE con un JSON válido con esta estructura:
                {
                  "tipo": "nombre_del_tipo_o_desconocido",
                  "periodo": "mes|anio|historico",
                  "mensaje_aclaracion": "Si tipo es desconocido, pregunta qué necesita, sino vacío",
                  "formato": "pdf|excel|word|pantalla"
                }
                """;
        return executePromptConFallback(systemPrompt, prompt);
    }
}