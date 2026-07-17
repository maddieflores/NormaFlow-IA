package com.workflow.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementación en memoria de {@link TokenBlacklist} para Ciclo 1.
 *
 * SRP: responsabilidad única — gestionar tokens invalidados por logout.
 * OCP: en producción multi-instancia, crear {@code RedisTokenBlacklist implements TokenBlacklist}
 *      sin modificar {@link InMemoryTokenBlacklist} ni sus clientes.
 *
 * Nota: esta implementación es válida para una sola instancia de la aplicación.
 * En un entorno de múltiples instancias (Kubernetes, etc.), usar Redis como almacén compartido.
 */
@Slf4j
@Service
public class InMemoryTokenBlacklist implements TokenBlacklist {

    /**
     * Clave: token JWT completo.
     * Valor: timestamp UNIX (segundos) de expiración del token.
     *
     * ConcurrentHashMap garantiza thread-safety sin bloqueo global.
     */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void invalidar(String token, long expiresAtEpoch) {
        blacklist.put(token, expiresAtEpoch);
        log.info("Token JWT invalidado por logout. Tokens en blacklist: {}", blacklist.size());
    }

    @Override
    public boolean estaInvalidado(String token) {
        return blacklist.containsKey(token);
    }

    /**
     * Limpieza periódica: elimina tokens cuya expiración natural ya pasó.
     * Son rechazados por la firma JWT de todas formas, por lo que mantenerlos es redundante.
     *
     * Se ejecuta automáticamente cada hora gracias a {@code @Scheduled} y
     * {@code @EnableScheduling} en {@link com.workflow.backend.BackendApplication}.
     */
    @Scheduled(fixedRate = 3_600_000)
    public void limpiarTokensExpirados() {
        long ahora = Instant.now().getEpochSecond();
        int antes = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() < ahora);
        int eliminados = antes - blacklist.size();
        if (eliminados > 0) {
            log.info("Blacklist: {} tokens expirados eliminados. Total restante: {}", eliminados, blacklist.size());
        }
    }
}
