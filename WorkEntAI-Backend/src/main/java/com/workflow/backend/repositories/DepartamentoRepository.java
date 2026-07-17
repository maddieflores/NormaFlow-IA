package com.workflow.backend.repositories;

import com.workflow.backend.models.Departamento;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DepartamentoRepository extends MongoRepository<Departamento, String> {
    Optional<Departamento> findByNombre(String nombre);
}
