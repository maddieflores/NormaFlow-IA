package com.workflow.backend.repositories;

import com.workflow.backend.models.DocumentoTramite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de documentos adjuntos a trámites (CU-24 al CU-27).
 * Principio SRP: solo acceso a datos de DocumentoTramite.
 */
@Repository
public interface DocumentoRepository extends MongoRepository<DocumentoTramite, String> {

    /** Obtiene todos los documentos activos de un trámite */
    List<DocumentoTramite> findByTramiteIdAndActivoTrue(String tramiteId);

    /** Obtiene documentos de un nodo específico dentro de un trámite */
    List<DocumentoTramite> findByTramiteIdAndNodoIdAndActivoTrue(String tramiteId, String nodoId);

    /** Cuenta versiones de un documento (por nombre y tramiteId) para versionamiento */
    long countByTramiteIdAndNombre(String tramiteId, String nombre);

    /** Todos los documentos de un trámite (incluyendo eliminados, para auditoría) */
    List<DocumentoTramite> findByTramiteId(String tramiteId);
}
