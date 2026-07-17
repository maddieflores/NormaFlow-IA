package com.workflow.backend.services;

/**
 * Contrato para la gestión de tokens JWT invalidados (blacklist).
 *
 * DIP: el JwtFilter y AuthController dependen de esta abstracción,
 * no de una implementación concreta.
 *
 * Implementaciones posibles:
 *  - {@link InMemoryTokenBlacklist} — Ciclo 1, una sola instancia (actual)
 *  - {@code RedisTokenBlacklist}    — Producción multi-instancia / cluster
 *
 * OCP: agregar RedisTokenBlacklist no requiere modificar JwtFilter ni AuthController.
 */
public interface TokenBlacklist {

    /**
     * Invalida un token JWT.
     *
     * @param token          Token JWT completo (sin prefijo "Bearer ")
     * @param expiresAtEpoch Timestamp UNIX en segundos de expiración natural del token
     */
    void invalidar(String token, long expiresAtEpoch);

    /**
     * Verifica si un token ha sido invalidado manualmente (logout).
     *
     * @param token Token JWT completo (sin prefijo "Bearer ")
     * @return {@code true} si fue invalidado; {@code false} si es válido
     */
    boolean estaInvalidado(String token);
}
