package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoAdjunto {

    private String id;
    private String nombreRequisito; // Referencia al nombre en RequisitoTramite
    private String nombreArchivoOriginal;
    private String s3Key;
    private String contentType;
    private Long size;
    private LocalDateTime fechaSubida;
}
