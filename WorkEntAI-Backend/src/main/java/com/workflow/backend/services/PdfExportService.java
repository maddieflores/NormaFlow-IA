package com.workflow.backend.services;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class PdfExportService {

    public byte[] generarPdf(String titulo, List<String> columnas, List<List<String>> filas) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph paragraphTitulo = new Paragraph(titulo, fontTitulo);
            paragraphTitulo.setAlignment(Paragraph.ALIGN_CENTER);
            paragraphTitulo.setSpacingAfter(20);
            document.add(paragraphTitulo);

            // Tabla
            PdfPTable table = new PdfPTable(columnas.size());
            table.setWidthPercentage(100);

            // Encabezados
            Font fontEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
            for (String col : columnas) {
                PdfPCell cell = new PdfPCell(new Phrase(col, fontEncabezado));
                cell.setBackgroundColor(new Color(15, 23, 42)); // Color oscuro slate-900
                cell.setPadding(8);
                cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Filas
            Font fontDatos = FontFactory.getFont(FontFactory.HELVETICA, 11);
            boolean alternate = false;
            for (List<String> fila : filas) {
                for (String celda : fila) {
                    PdfPCell cell = new PdfPCell(new Phrase(celda != null ? celda : "", fontDatos));
                    cell.setPadding(6);
                    if (alternate) {
                        cell.setBackgroundColor(new Color(248, 250, 252)); // Color slate-50
                    }
                    table.addCell(cell);
                }
                alternate = !alternate;
            }

            document.add(table);
            document.close();
            
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage(), e);
        }
    }
}
