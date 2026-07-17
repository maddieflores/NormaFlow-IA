package com.workflow.backend.repositories;

import com.workflow.backend.models.AgenteSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de sesiones del Agente Inteligente (CU-22/23).
 */
@Repository
public interface AgenteRepository extends MongoRepository<AgenteSession, String> {

    /** Busca la sesión activa más reciente de un cliente */
    Optional<AgenteSession> findTopByClienteIdAndActivaTrueOrderByFechaCreacionDesc(String clienteId);

    /** Todas las sesiones de un cliente (historial) */
    List<AgenteSession> findByClienteIdOrderByFechaCreacionDesc(String clienteId);
}
