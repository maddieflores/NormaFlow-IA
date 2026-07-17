package com.workflow.backend.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.models.HistorialTramite;
import com.workflow.backend.models.Tramite;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generarPdfTramite(Tramite tramite) {
        // Validate estado == COMPLETADO
        if (tramite.getEstado() != EstadoTramite.COMPLETADO) {
            throw new BusinessException("Solo se puede generar PDF para trámites completados");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);
            
            document.open();

            // 1. Header: title "Resultado del Trámite" + numeroReferencia
            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Resultado del Trámite", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Font refFont = new Font(Font.HELVETICA, 14, Font.NORMAL);
            Paragraph reference = new Paragraph(tramite.getNumeroReferencia(), refFont);
            reference.setAlignment(Element.ALIGN_CENTER);
            reference.setSpacingAfter(20);
            document.add(reference);

            // 2. Metadata table
            document.add(new Paragraph("Información General", new Font(Font.HELVETICA, 12, Font.BOLD)));
            document.add(Chunk.NEWLINE);
            
            PdfPTable metadataTable = new PdfPTable(2);
            metadataTable.setWidthPercentage(100);
            metadataTable.setSpacingAfter(20);
            
            addMetadataRow(metadataTable, "Política", tramite.getNombrePolitica());
            addMetadataRow(metadataTable, "Cliente", tramite.getNombreCliente());
            addMetadataRow(metadataTable, "Fecha de Inicio", 
                tramite.getFechaInicio() != null ? tramite.getFechaInicio().format(DATE_FORMATTER) : "N/A");
            addMetadataRow(metadataTable, "Fecha de Fin", 
                tramite.getFechaFin() != null ? tramite.getFechaFin().format(DATE_FORMATTER) : "N/A");
            addMetadataRow(metadataTable, "Duración Total", 
                tramite.getDuracionMinutos() != null ? tramite.getDuracionMinutos() + " minutos" : "N/A");
            
            document.add(metadataTable);

            // 3. Historial table
            if (tramite.getHistorial() != null && !tramite.getHistorial().isEmpty()) {
                document.add(new Paragraph("Historial de Pasos", new Font(Font.HELVETICA, 12, Font.BOLD)));
                document.add(Chunk.NEWLINE);
                
                PdfPTable historialTable = new PdfPTable(7);
                historialTable.setWidthPercentage(100);
                historialTable.setSpacingAfter(20);
                historialTable.setWidths(new float[]{1, 3, 2, 2, 2, 2, 2});
                
                // Headers
                addHeaderCell(historialTable, "#");
                addHeaderCell(historialTable, "Nodo");
                addHeaderCell(historialTable, "Departamento");
                addHeaderCell(historialTable, "Funcionario");
                addHeaderCell(historialTable, "Acción");
                addHeaderCell(historialTable, "Fecha");
                addHeaderCell(historialTable, "Duración");
                
                // Data rows
                int stepNumber = 1;
                for (HistorialTramite hist : tramite.getHistorial()) {
                    addDataCell(historialTable, String.valueOf(stepNumber++));
                    addDataCell(historialTable, hist.getNombreNodo() != null ? hist.getNombreNodo() : "N/A");
                    addDataCell(historialTable, hist.getDepartamento() != null ? hist.getDepartamento() : "N/A");
                    addDataCell(historialTable, hist.getNombreFuncionario() != null ? hist.getNombreFuncionario() : "N/A");
                    addDataCell(historialTable, hist.getAccion() != null ? hist.getAccion() : "N/A");
                    addDataCell(historialTable, hist.getFecha() != null ? hist.getFecha().format(DATE_FORMATTER) : "N/A");
                    addDataCell(historialTable, hist.getDuracionMinutos() != null ? hist.getDuracionMinutos() + " min" : "N/A");
                }
                
                document.add(historialTable);
            }

            // 4. datosFormulario section
            if (tramite.getDatosFormulario() != null && !tramite.getDatosFormulario().isEmpty()) {
                document.add(new Paragraph("Datos del Formulario", new Font(Font.HELVETICA, 12, Font.BOLD)));
                document.add(Chunk.NEWLINE);
                
                PdfPTable formTable = new PdfPTable(2);
                formTable.setWidthPercentage(100);
                formTable.setWidths(new float[]{1, 2});
                
                // Headers
                addHeaderCell(formTable, "Campo");
                addHeaderCell(formTable, "Valor");
                
                // Data rows
                for (Map.Entry<String, Object> entry : tramite.getDatosFormulario().entrySet()) {
                    addDataCell(formTable, entry.getKey());
                    addDataCell(formTable, entry.getValue() != null ? entry.getValue().toString() : "N/A");
                }
                
                document.add(formTable);
            }

            document.close();
            return baos.toByteArray();
            
        } catch (DocumentException e) {
            throw new BusinessException("Error al generar el PDF: " + e.getMessage());
        }
    }

    private void addMetadataRow(PdfPTable table, String label, String value) {
        Font labelFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL);
        
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "N/A", valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addHeaderCell(PdfPTable table, String text) {
        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        PdfPCell cell = new PdfPCell(new Phrase(text, headerFont));
        cell.setBackgroundColor(new Color(200, 200, 200));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text) {
        Font dataFont = new Font(Font.HELVETICA, 8, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(text, dataFont));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
