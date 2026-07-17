package com.workflow.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de blacklist de tokens JWT para soporte de logout seguro (CU-02).
 *
 * Mantiene un mapa en memoria de tokens invalidados con su timestamp de expiración.
 * Un scheduler limpia tokens ya expirados cada hora para evitar crecimiento indefinido.
 *
 * Nota: En un entorno multi-instancia (cluster), este mapa debería persistirse en Redis.
 * Para Ciclo 1 con una única instancia, la solución en memoria es suficiente.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    // Clave: token JWT completo. Valor: epoch en segundos de cuando expira el token.
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    /**
     * Invalida un token JWT.
     * A partir de este momento, cualquier request con este token será rechazado con 401.
     *
     * @param token       El token JWT a invalidar (sin el prefijo "Bearer ")
     * @param expiresAtEpoch Timestamp UNIX (segundos) en el que el token vence naturalmente
     */
    public void invalidar(String token, long expiresAtEpoch) {
        blacklist.put(token, expiresAtEpoch);
        log.info("Token JWT invalidado. Total tokens en blacklist: {}", blacklist.size());
    }

    /**
     * Comprueba si un token está en la blacklist.
     *
     * @param token El token JWT (sin "Bearer ")
     * @return true si el token fue invalidado manualmente (logout)
     */
    public boolean estaInvalidado(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Limpia tokens expirados de la blacklist cada hora.
     * Los tokens expirados son rechazados por la firma JWT de todas formas,
     * por lo que mantenerlos en el mapa es redundante.
     */
    @Scheduled(fixedRate = 3_600_000) // cada 1 hora
    public void limpiarExpirados() {
        long ahora = Instant.now().getEpochSecond();
        int antes = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < ahora);
        int eliminados = antes - blacklist.size();
        if (eliminados > 0) {
            log.info("Blacklist limpieza: {} tokens expirados eliminados. Total restante: {}",
                    eliminados, blacklist.size());
        }
    }
}
