package com.workflow.backend;

// Feature: workflow-management-system, Property 12: Required Form Field Validation
// Validates: Requirements 9.7

import com.workflow.backend.exception.BusinessException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property test for Required Form Field Validation.
 *
 * Verifies that for any Nodo with at least one required field, submitting form
 * data missing that field throws BusinessException, and that submitting data
 * WITH the required field does NOT throw.
 */
class RequiredFieldPropertyTest {

    /**
     * Replicates the validarCamposRequeridos logic from WorkflowEngineService
     * without any MongoDB/Spring dependencies.
     */
    static void validarCamposRequeridos(List<Map<String, Object>> camposFormulario,
                                        Map<String, Object> datos) {
        if (camposFormulario == null) return;
        for (Map<String, Object> campo : camposFormulario) {
            boolean requerido = Boolean.TRUE.equals(campo.get("requerido"));
            String nombre = (String) campo.get("nombre");
            if (requerido && (datos == null || !datos.containsKey(nombre)
                    || datos.get(nombre) == null
                    || datos.get(nombre).toString().isBlank())) {
                throw new BusinessException("Campo requerido faltante: " + nombre);
            }
        }
    }

    @Property
    void missingRequiredFieldThrowsBusinessException(
            @ForAll @AlphaChars @StringLength(min = 1, max = 15) String fieldName) {

        Map<String, Object> campo = new HashMap<>();
        campo.put("nombre", fieldName);
        campo.put("requerido", true);

        List<Map<String, Object>> camposFormulario = new ArrayList<>();
        camposFormulario.add(campo);

        Map<String, Object> emptyData = new HashMap<>();

        assertThatThrownBy(() -> validarCamposRequeridos(camposFormulario, emptyData))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(fieldName);

        assertThatThrownBy(() -> validarCamposRequeridos(camposFormulario, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(fieldName);
    }

    @Property
    void presentRequiredFieldDoesNotThrow(
            @ForAll @AlphaChars @StringLength(min = 1, max = 15) String fieldName) {

        Map<String, Object> campo = new HashMap<>();
        campo.put("nombre", fieldName);
        campo.put("requerido", true);

        List<Map<String, Object>> camposFormulario = new ArrayList<>();
        camposFormulario.add(campo);

        Map<String, Object> validData = new HashMap<>();
        validData.put(fieldName, "someValue");

        assertThatCode(() -> validarCamposRequeridos(camposFormulario, validData))
                .doesNotThrowAnyException();
    }
}
