package com.workflow.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteService {

    private final com.workflow.backend.ai.AIService aiService;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public Map<String, Object> procesarSolicitudReporte(String prompt) {
        Map<String, Object> respuesta = new HashMap<>();
        
        try {
            // 1. Interpretar con NLP
            String jsonAi = aiService.analizarSolicitudReporte(prompt);
            // Limpiar posibles bloques markdown del JSON
            jsonAi = jsonAi.replace("```json", "").replace("```", "").trim();
            
            JsonNode requestInfo = objectMapper.readTree(jsonAi);
            String tipo = requestInfo.path("tipo").asText("desconocido");
            String mensajeAclaracion = requestInfo.path("mensaje_aclaracion").asText("");
            
            respuesta.put("tipo_detectado", tipo);
            
            if ("desconocido".equals(tipo)) {
                respuesta.put("status", "ACLARACION");
                respuesta.put("mensaje", mensajeAclaracion);
                return respuesta;
            }

            // 2. Ejecutar Aggregation según tipo
            List<Map<String, Object>> resultados = ejecutarConsultaAgregada(tipo);
            
            respuesta.put("status", "OK");
            respuesta.put("datos", resultados);
            
            // Format for Excel Export compatibility
            List<String> columnas = new java.util.ArrayList<>();
            List<List<String>> filas = new java.util.ArrayList<>();
            if (!resultados.isEmpty()) {
                Map<String, Object> firstRow = resultados.get(0);
                columnas.add("Nº");
                columnas.addAll(firstRow.keySet());
                
                int rowIndex = 1;
                for (Map<String, Object> row : resultados) {
                    List<String> filaStr = new java.util.ArrayList<>();
                    filaStr.add(String.valueOf(rowIndex++));
                    for (String col : firstRow.keySet()) {
                        Object val = row.get(col);
                        if (val instanceof List<?> listVal) {
                            List<String> stringList = listVal.stream().map(String::valueOf).toList();
                            filaStr.add(String.join(", ", stringList));
                        } else {
                            filaStr.add(val != null ? String.valueOf(val) : "N/A");
                        }
                    }
                    filas.add(filaStr);
                }
            } else {
                columnas.add("Resultado");
                filas.add(List.of("No se encontraron datos."));
            }
            respuesta.put("titulo", "Reporte de " + tipo.replace("_", " ").toUpperCase());
            respuesta.put("columnas", columnas);
            respuesta.put("filas", filas);
            respuesta.put("mensaje", "Reporte generado correctamente.");
            
        } catch (Exception e) {
            log.error("Error al procesar reporte: {}", e.getMessage(), e);
            respuesta.put("status", "ERROR");
            respuesta.put("mensaje", "No se pudo generar el reporte: " + e.getMessage());
        }
        
        return respuesta;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> ejecutarConsultaAgregada(String tipo) {
        Aggregation agg;
        switch (tipo) {
            case "politica_mas_utilizada":
                agg = newAggregation(
                    group("nombrePolitica").count().as("Total Trámites"),
                    sort(Sort.Direction.DESC, "Total Trámites"),
                    limit(10),
                    project().andExclude("_id").and("_id").as("Política").andInclude("Total Trámites")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "tramites", Map.class).getMappedResults();
                
            case "cliente_mas_tramites":
                agg = newAggregation(
                    addFields().addFieldWithValue("clienteObjId", org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToObjectId.toObjectId("$clienteId")).build(),
                    lookup("usuarios", "clienteObjId", "_id", "cliente_info"),
                    unwind("cliente_info", true),
                    group("clienteId")
                        .first("nombreCliente").as("Cliente")
                        .first("cliente_info.email").as("Correo")
                        .count().as("Total Trámites"),
                    sort(Sort.Direction.DESC, "Total Trámites"),
                    limit(10),
                    project().andExclude("_id").andInclude("Cliente", "Correo", "Total Trámites")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "tramites", Map.class).getMappedResults();
                
            case "tiempo_promedio_depto":
                agg = newAggregation(
                    group("departamento").count().as("Total Tareas"),
                    sort(Sort.Direction.DESC, "Total Tareas"),
                    project().andExclude("_id").and("_id").as("Departamento").andInclude("Total Tareas")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "tareas", Map.class).getMappedResults();
                
            case "eficiencia_funcionarios":
                agg = newAggregation(
                    addFields().addFieldWithValue("funcionarioObjId", org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToObjectId.toObjectId("$funcionarioId")).build(),
                    lookup("usuarios", "funcionarioObjId", "_id", "usuario_info"),
                    unwind("usuario_info", true),
                    group("funcionarioId")
                        .first("nombreFuncionario").as("Nombre")
                        .first("usuario_info.email").as("Correo")
                        .first("departamento").as("Departamento")
                        .addToSet("nombreNodo").as("Tareas Asignadas")
                        .addToSet("nombrePolitica").as("Políticas")
                        .count().as("Total Tareas"),
                    sort(Sort.Direction.DESC, "Total Tareas"),
                    project().andExclude("_id").andInclude("Nombre", "Correo", "Departamento", "Total Tareas", "Tareas Asignadas", "Políticas")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "tareas", Map.class).getMappedResults();
                
            case "documentos_generados":
                agg = newAggregation(
                    addFields().addFieldWithValue("tramiteObjId", org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToObjectId.toObjectId("$tramiteId")).build(),
                    lookup("tramites", "tramiteObjId", "_id", "tramite_info"),
                    unwind("tramite_info", true),
                    group("tramiteId")
                        .first("tramite_info.numeroReferencia").as("Referencia")
                        .first("tramite_info.descripcion").as("Descripción")
                        .count().as("Total Documentos"),
                    sort(Sort.Direction.DESC, "Total Documentos"),
                    limit(15),
                    project().andExclude("_id").andInclude("Referencia", "Descripción", "Total Documentos")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "documentos_tramite", Map.class).getMappedResults();
                
            case "tramites_riesgo_demora":
            default:
                agg = newAggregation(
                    match(org.springframework.data.mongodb.core.query.Criteria.where("estado").is("EN_PROCESO")),
                    sort(Sort.Direction.ASC, "fechaInicio"),
                    limit(20),
                    project()
                        .andExclude("_id")
                        .and("numeroReferencia").as("Referencia")
                        .and("descripcion").as("Descripción")
                        .and("nombreCliente").as("Cliente")
                        .and("nombrePolitica").as("Política")
                        .and("estado").as("Estado")
                        .and("fechaInicio").as("Fecha Inicio")
                );
                return (List<Map<String, Object>>)(List<?>) mongoTemplate.aggregate(agg, "tramites", Map.class).getMappedResults();
        }
    }
}
