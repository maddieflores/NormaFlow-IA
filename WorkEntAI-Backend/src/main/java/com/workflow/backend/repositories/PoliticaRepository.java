package com.workflow.backend.repositories;

import com.workflow.backend.models.Politica;
import com.workflow.backend.enums.EstadoPolitica;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PoliticaRepository extends MongoRepository<Politica, String> {
    List<Politica> findByEstado(EstadoPolitica estado);
    List<Politica> findByCreadoPorId(String creadoPorId);
}