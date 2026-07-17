package com.workflow.backend.services;

import com.workflow.backend.models.DocumentoTramite;
import com.workflow.backend.repositories.AuditoriaDocumentoRepository;
import com.workflow.backend.repositories.DocumentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentoServiceTest {

    @Mock
    private DocumentoRepository documentoRepository;

    @Mock
    private AuditoriaDocumentoRepository auditoriaRepository;

    @Mock
    private ArchivoStorage archivoStorage;

    @InjectMocks
    private DocumentoService documentoService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void testSubirDocumento() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "dummy content".getBytes());

        when(archivoStorage.guardar(any(), any())).thenReturn("tramites/t1/123-test.pdf");
        when(documentoRepository.countByTramiteIdAndNombre(anyString(), anyString())).thenReturn(0L);
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            DocumentoTramite d = inv.getArgument(0);
            d.setId("doc1");
            return d;
        });

        DocumentoTramite doc = documentoService.subirDocumento(file, "t1", "n1", "desc", "u1", "Juan");

        assertNotNull(doc);
        assertEquals("test.pdf", doc.getNombre());
        assertEquals(1, doc.getVersion());
        assertEquals("tramites/t1/123-test.pdf", doc.getS3Key());

        verify(auditoriaRepository).save(any());
        verify(documentoRepository).save(any());
    }

    @Test
    void testObtenerUrlDescargaConPermisoAdmin() {
        DocumentoTramite doc = DocumentoTramite.builder()
                .id("doc1")
                .tramiteId("t1")
                .s3Key("key1")
                .activo(true)
                .permisos(java.util.List.of())
                .build();

        when(documentoRepository.findById("doc1")).thenReturn(Optional.of(doc));
        when(archivoStorage.generarUrl("key1")).thenReturn("http://s3.url/key1");

        String url = documentoService.obtenerUrlDescarga("doc1", "u1", "Juan", "ADMIN");

        assertEquals("http://s3.url/key1", url);
        verify(auditoriaRepository).save(argThat(a -> a.getAccion().equals("VER_URL")));
    }
}
