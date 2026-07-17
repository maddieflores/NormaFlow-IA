package com.workflow.backend.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermisoDocumental {
    @Builder.Default
    private boolean lectura = true;
    
    @Builder.Default
    private boolean escritura = false;
    
    @Builder.Default
    private boolean subida = false;
}
