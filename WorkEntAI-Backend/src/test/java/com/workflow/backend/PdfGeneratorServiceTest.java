package com.workflow.backend;

import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.exception.BusinessException;
import com.workflow.backend.models.HistorialTramite;
import com.workflow.backend.models.Tramite;
import com.workflow.backend.services.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PdfGeneratorService.
 * Validates Requirements 15.1, 15.3
 */
class PdfGeneratorServiceTest {

    private PdfGeneratorService pdfGeneratorService;

    @BeforeEach
    void setUp() {
        pdfGeneratorService = new PdfGeneratorService();
    }

    /**
     * Test that generarPdfTramite() returns a non-empty byte array for a valid completed Trámite.
     * Validates: Requirements 15.1
     */
    @Test
    void generarPdfTramite_completado_returnsNonEmptyByteArray() {
        HistorialTramite historial = HistorialTramite.builder()
                .nombreNodo("Revisión Inicial")
                .departamento("Atención al Cliente")
                .nombreFuncionario("Juan Pérez")
                .accion("COMPLETADO")
                .duracionMinutos(15L)
                .fecha(LocalDateTime.now().minusHours(1))
                .build();

        Tramite tramite = Tramite.builder()
                .estado(EstadoTramite.COMPLETADO)
                .numeroReferencia("TRM-2026-0001")
                .nombrePolitica("Política de Crédito")
                .nombreCliente("María García")
                .fechaInicio(LocalDateTime.now().minusHours(2))
                .fechaFin(LocalDateTime.now())
                .duracionMinutos(120L)
                .historial(List.of(historial))
                .datosFormulario(Map.of("monto", "5000", "plazo", "12 meses"))
                .build();

        byte[] result = pdfGeneratorService.generarPdfTramite(tramite);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    /**
     * Test that generarPdfTramite() throws BusinessException for a non-COMPLETADO Trámite.
     * Validates: Requirements 15.3
     */
    @Test
    void generarPdfTramite_enProceso_throwsBusinessException() {
        Tramite tramite = Tramite.builder()
                .estado(EstadoTramite.EN_PROCESO)
                .numeroReferencia("TRM-2026-0002")
                .build();

        assertThrows(BusinessException.class, () -> pdfGeneratorService.generarPdfTramite(tramite));
    }
}
