package com.workflow.backend.services;

import com.workflow.backend.models.CondicionNodo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Evaluador de condiciones para nodos de tipo DECISION (CU-05 — Opción B).
 *
 * Responsabilidad única (SRP): contiene ÚNICAMENTE la lógica de evaluación de
 * expresiones de campo/operador/valor. Extraído de WorkflowEngineService para
 * que este servicio no crezca con lógica de evaluación.
 *
 * Abierto para extensión (OCP): nuevos operadores se agregan aquí sin modificar
 * el motor de workflow.
 *
 * Operadores soportados: ==  !=  >  <  >=  <=  contains
 */
@Slf4j
@Component
public class CondicionEvaluador {

    /**
     * Evalúa una condición de expresión contra los datos del formulario.
     *
     * Busca el valor del campo primero en {@code datosRecien} (formulario recién completado)
     * y, si no lo encuentra, en {@code datosAcumulados} (historial del trámite).
     *
     * @param cond           La condición a evaluar (campo, operador, valor, nodoSiId, nodoNoId)
     * @param datosRecien    Datos del formulario del nodo que acaba de completarse
     * @param datosAcumulados Datos acumulados de todos los nodos anteriores del trámite
     * @return {@code true} si la condición se cumple; {@code false} en caso contrario
     */
    public boolean evaluar(CondicionNodo cond,
                           Map<String, Object> datosRecien,
                           Map<String, Object> datosAcumulados) {

        Object valorCampoObj = resolverValorCampo(cond.getCampo(), datosRecien, datosAcumulados);

        if (valorCampoObj == null) {
            log.warn("Evaluador DECISION: campo '{}' no encontrado en el formulario. Resultado: false.",
                    cond.getCampo());
            return false;
        }

        String valorCampo = valorCampoObj.toString().trim();
        String valorRef   = cond.getValor()    != null ? cond.getValor().trim()    : "";
        String operador   = cond.getOperador() != null ? cond.getOperador().trim() : "==";

        boolean resultado = aplicarOperador(operador, valorCampo, valorRef);

        log.info("Evaluador DECISION — campo='{}' operador='{}' valorRef='{}' valorCampo='{}' → {}",
                cond.getCampo(), operador, valorRef, valorCampo, resultado);

        return resultado;
    }

    // ── Resolución del valor del campo ───────────────────────────────────────

    private Object resolverValorCampo(String campo,
                                       Map<String, Object> datosRecien,
                                       Map<String, Object> datosAcumulados) {
        if (datosRecien != null && datosRecien.containsKey(campo)) {
            return datosRecien.get(campo);
        }
        if (datosAcumulados != null && datosAcumulados.containsKey(campo)) {
            return datosAcumulados.get(campo);
        }
        return null;
    }

    // ── Aplicación del operador ──────────────────────────────────────────────

    private boolean aplicarOperador(String operador, String valorCampo, String valorRef) {
        return switch (operador) {
            case "==" -> valorCampo.equalsIgnoreCase(valorRef);
            case "!=" -> !valorCampo.equalsIgnoreCase(valorRef);
            case "contains" -> valorCampo.toLowerCase().contains(valorRef.toLowerCase());
            case ">", "<", ">=", "<=" -> evaluarComparacionNumerica(valorCampo, operador, valorRef);
            default -> {
                log.warn("Operador desconocido '{}' en condición DECISION. Resultado: false.", operador);
                yield false;
            }
        };
    }

    private boolean evaluarComparacionNumerica(String valorCampo, String operador, String valorRef) {
        try {
            double v1 = Double.parseDouble(valorCampo);
            double v2 = Double.parseDouble(valorRef);
            return switch (operador) {
                case ">"  -> v1 > v2;
                case "<"  -> v1 < v2;
                case ">=" -> v1 >= v2;
                case "<=" -> v1 <= v2;
                default   -> false;
            };
        } catch (NumberFormatException e) {
            log.warn("No se pudo comparar numéricamente '{}' {} '{}': valores no numéricos.",
                    valorCampo, operador, valorRef);
            return false;
        }
    }
}
