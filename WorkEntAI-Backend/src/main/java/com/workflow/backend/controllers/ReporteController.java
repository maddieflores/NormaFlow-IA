package com.workflow.backend.controllers;

import com.workflow.backend.services.ReporteService;
import com.workflow.backend.services.ExcelExportService;
import com.workflow.backend.services.WordExportService;
import com.workflow.backend.services.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;
    private final ExcelExportService excelExportService;
    private final WordExportService wordExportService;
    private final PdfExportService pdfExportService;

    // Caché en memoria de reportes generados en la sesión (por requestId)
    private final Map<String, Map<String, Object>> reportesCache = new java.util.concurrent.ConcurrentHashMap<>();

    @PostMapping("/generar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generarReporte(
            @RequestBody Map<String, String> body) {

        String solicitud = body.getOrDefault("solicitud", "").trim();
        if (solicitud.isBlank()) {
            solicitud = body.getOrDefault("prompt", "").trim(); // fallback
        }
        if (solicitud.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "ERROR", "mensaje", "La solicitud no puede estar vacía"));
        }

        log.info("📊 Generando reporte NLP con Mongo Aggregation: '{}'", solicitud);

        Map<String, Object> reporte = reporteService.procesarSolicitudReporte(solicitud);

        if ("OK".equals(reporte.get("status"))) {
            // Guardar en cache para posible exportación Excel/PDF
            String reporteId = UUID.randomUUID().toString();
            reporte.put("reporteId", reporteId);
            reportesCache.put(reporteId, reporte);
        }

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/{reporteId}/excel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarExcel(@PathVariable String reporteId) throws IOException {

        Map<String, Object> reporte = reportesCache.get(reporteId);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }

        String titulo = (String) reporte.getOrDefault("titulo", "Reporte WorkEntAI");

        @SuppressWarnings("unchecked")
        List<String> columnas = (List<String>) reporte.getOrDefault("columnas", List.of("Datos"));

        List<List<String>> filas = ((List<?>) reporte.getOrDefault("filas", new ArrayList<>()))
                .stream()
                .map(f -> {
                    if (f instanceof List<?> lista) {
                        return lista.stream().map(Object::toString).toList();
                    }
                    return List.of(f.toString());
                })
                .toList();

        byte[] excelBytes = excelExportService.generarExcel(titulo, columnas, filas);

        String filename = "reporte-workentai-" + reporteId.substring(0, 8) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(excelBytes.length)
                .body(excelBytes);
    }

    @GetMapping("/{reporteId}/word")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarWord(@PathVariable String reporteId) throws IOException {

        Map<String, Object> reporte = reportesCache.get(reporteId);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }

        String titulo = (String) reporte.getOrDefault("titulo", "Reporte WorkEntAI");

        @SuppressWarnings("unchecked")
        List<String> columnas = (List<String>) reporte.getOrDefault("columnas", List.of("Datos"));

        List<List<String>> filas = ((List<?>) reporte.getOrDefault("filas", new ArrayList<>()))
                .stream()
                .map(f -> {
                    if (f instanceof List<?> lista) {
                        return lista.stream().map(Object::toString).toList();
                    }
                    return List.of(f.toString());
                })
                .toList();

        byte[] wordBytes = wordExportService.generarWord(titulo, columnas, filas);

        String filename = "reporte-workentai-" + reporteId.substring(0, 8) + ".docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(wordBytes.length)
                .body(wordBytes);
    }

    @GetMapping("/{reporteId}/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarPdf(@PathVariable String reporteId) throws IOException {

        Map<String, Object> reporte = reportesCache.get(reporteId);
        if (reporte == null) {
            return ResponseEntity.notFound().build();
        }

        String titulo = (String) reporte.getOrDefault("titulo", "Reporte WorkEntAI");

        @SuppressWarnings("unchecked")
        List<String> columnas = (List<String>) reporte.getOrDefault("columnas", List.of("Datos"));

        List<List<String>> filas = ((List<?>) reporte.getOrDefault("filas", new ArrayList<>()))
                .stream()
                .map(f -> {
                    if (f instanceof List<?> lista) {
                        return lista.stream().map(Object::toString).toList();
                    }
                    return List.of(f.toString());
                })
                .toList();

        byte[] pdfBytes = pdfExportService.generarPdf(titulo, columnas, filas);

        String filename = "reporte-workentai-" + reporteId.substring(0, 8) + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }
}