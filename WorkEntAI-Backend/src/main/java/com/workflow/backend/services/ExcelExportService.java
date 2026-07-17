package com.workflow.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Servicio de exportación de reportes a Excel (Ciclo 2 — CU-30).
 *
 * Principio SRP: única responsabilidad de generar archivos .xlsx a partir de datos tabulares.
 * Usa Apache POI (poi-ooxml) con estilo moderno: cabecera con color corporativo,
 * filas alternas, ancho automático de columnas.
 */
@Slf4j
@Service
public class ExcelExportService {

    private static final String COLOR_HEADER = "2563EB";   // Azul corporativo WorkEntAI
    private static final String COLOR_FILA_PAR = "EFF6FF";  // Azul muy claro
    private static final String FONT_NAME = "Calibri";

    /**
     * Genera un archivo Excel (.xlsx) con los datos del reporte.
     *
     * @param titulo    Título del reporte (se escribe en la primera fila)
     * @param columnas  Nombres de las columnas
     * @param filas     Filas de datos (cada fila es una lista de valores)
     * @return Bytes del archivo .xlsx listo para descarga
     */
    public byte[] generarExcel(String titulo, List<String> columnas,
                                List<List<String>> filas) throws IOException {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Reporte");

            // ── Estilos ───────────────────────────────────────────────────────
            XSSFCellStyle estiloTitulo = crearEstiloTitulo(workbook);
            XSSFCellStyle estiloCabecera = crearEstiloCabecera(workbook);
            XSSFCellStyle estiloFilaNormal = crearEstiloFila(workbook, false);
            XSSFCellStyle estiloFilaPar = crearEstiloFila(workbook, true);

            int filaActual = 0;

            // ── Fila de título ────────────────────────────────────────────────
            Row rowTitulo = sheet.createRow(filaActual++);
            rowTitulo.setHeightInPoints(30);
            Cell cellTitulo = rowTitulo.createCell(0);
            cellTitulo.setCellValue(titulo);
            cellTitulo.setCellStyle(estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, columnas.size() - 1));

            // ── Fila vacía separadora ─────────────────────────────────────────
            sheet.createRow(filaActual++);

            // ── Fila de cabecera de columnas ──────────────────────────────────
            Row rowCabecera = sheet.createRow(filaActual++);
            rowCabecera.setHeightInPoints(20);
            for (int i = 0; i < columnas.size(); i++) {
                Cell cell = rowCabecera.createCell(i);
                cell.setCellValue(columnas.get(i));
                cell.setCellStyle(estiloCabecera);
            }

            // ── Filas de datos ────────────────────────────────────────────────
            for (int i = 0; i < filas.size(); i++) {
                Row row = sheet.createRow(filaActual++);
                row.setHeightInPoints(16);
                List<String> fila = filas.get(i);
                XSSFCellStyle estilo = (i % 2 == 0) ? estiloFilaNormal : estiloFilaPar;
                for (int j = 0; j < fila.size(); j++) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(fila.get(j));
                    cell.setCellStyle(estilo);
                }
            }

            // ── Ajustar ancho de columnas automáticamente ─────────────────────
            for (int i = 0; i < columnas.size(); i++) {
                sheet.autoSizeColumn(i);
                // Mínimo 12 caracteres, máximo 50
                int ancho = Math.min(Math.max(sheet.getColumnWidth(i), 3000), 15000);
                sheet.setColumnWidth(i, ancho);
            }

            workbook.write(out);
            log.info("📊 Excel generado: '{}' con {} filas y {} columnas",
                    titulo, filas.size(), columnas.size());
            return out.toByteArray();
        }
    }

    // ── Helpers de estilo ─────────────────────────────────────────────────────

    private XSSFCellStyle crearEstiloTitulo(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(new XSSFColor(hexToBytes("FFFFFF"), null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(hexToBytes(COLOR_HEADER), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private XSSFCellStyle crearEstiloCabecera(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName(FONT_NAME);
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(hexToBytes("FFFFFF"), null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(hexToBytes("1E40AF"), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(new XSSFColor(hexToBytes("BFDBFE"), null));
        return style;
    }

    private XSSFCellStyle crearEstiloFila(XSSFWorkbook wb, boolean alterno) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        if (alterno) {
            style.setFillForegroundColor(new XSSFColor(hexToBytes(COLOR_FILA_PAR), null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(new XSSFColor(hexToBytes("E2E8F0"), null));
        return style;
    }

    private byte[] hexToBytes(String hex) {
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new byte[]{(byte) r, (byte) g, (byte) b};
    }
}
