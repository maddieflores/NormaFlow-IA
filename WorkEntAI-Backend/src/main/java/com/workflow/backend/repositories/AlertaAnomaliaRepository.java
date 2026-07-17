package com.workflow.backend.repositories;

import com.workflow.backend.models.AlertaAnomalia;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaAnomaliaRepository extends MongoRepository<AlertaAnomalia, String> {
    List<AlertaAnomalia> findByResueltaFalseOrderByFechaDeteccionDesc();
    List<AlertaAnomalia> findByPoliticaIdOrderByFechaDeteccionDesc(String politicaId);
}
