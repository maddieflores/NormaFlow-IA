package com.workflow.backend.repositories;

import com.workflow.backend.models.AuditoriaDocumento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de auditoría de documentos (CU-27).
 * Los registros son inmutables — nunca se eliminan.
 */
@Repository
public interface AuditoriaDocumentoRepository extends MongoRepository<AuditoriaDocumento, String> {

    /** Historial completo de accesos a un documento específico */
    List<AuditoriaDocumento> findByDocumentoIdOrderByTimestampDesc(String documentoId);

    /** Todos los eventos de auditoría de un trámite */
    List<AuditoriaDocumento> findByTramiteIdOrderByTimestampDesc(String tramiteId);

    /** Eventos de un usuario específico */
    List<AuditoriaDocumento> findByUsuarioIdOrderByTimestampDesc(String usuarioId);
}
