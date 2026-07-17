package com.workflow.backend.repositories;

import com.workflow.backend.models.Tramite;
import com.workflow.backend.enums.EstadoTramite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TramiteRepository extends MongoRepository<Tramite, String> {
    List<Tramite> findByClienteId(String clienteId);
    List<Tramite> findByEstado(EstadoTramite estado);
    List<Tramite> findByPoliticaId(String politicaId);
    List<Tramite> findByNodoActualId(String nodoActualId);
    Optional<Tramite> findByNumeroReferencia(String numeroReferencia);
}