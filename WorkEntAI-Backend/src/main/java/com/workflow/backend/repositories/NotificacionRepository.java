package com.workflow.backend.repositories;

import com.workflow.backend.models.Notificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificacionRepository extends MongoRepository<Notificacion, String> {
    List<Notificacion> findByUsuarioId(String usuarioId);
    List<Notificacion> findByUsuarioIdAndLeida(String usuarioId, boolean leida);
}