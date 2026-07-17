package com.workflow.backend.services;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class WordExportService {

    public byte[] generarWord(String titulo, List<String> columnas, List<List<String>> filas) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Título del documento
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(titulo);
            titleRun.setBold(true);
            titleRun.setFontSize(20);
            titleRun.setFontFamily("Arial");

            document.createParagraph(); // Espacio en blanco

            // Crear la tabla
            XWPFTable table = document.createTable();
            
            // Fila de encabezado
            XWPFTableRow headerRow = table.getRow(0);
            for (int i = 0; i < columnas.size(); i++) {
                XWPFTableCell cell;
                if (i == 0) {
                    cell = headerRow.getCell(0);
                } else {
                    cell = headerRow.addNewTableCell();
                }
                cell.setColor("E2E8F0"); // Fondo gris claro
                XWPFParagraph p = cell.getParagraphs().get(0);
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun r = p.createRun();
                r.setBold(true);
                r.setText(columnas.get(i));
            }

            // Filas de datos
            for (List<String> fila : filas) {
                XWPFTableRow row = table.createRow();
                for (int i = 0; i < fila.size(); i++) {
                    XWPFTableCell cell = row.getCell(i);
                    XWPFParagraph p = cell.getParagraphs().get(0);
                    XWPFRun r = p.createRun();
                    r.setText(fila.get(i) != null ? fila.get(i) : "");
                }
            }

            document.write(out);
            return out.toByteArray();
        }
    }
}
