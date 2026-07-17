package com.workflow.backend.repositories;

import com.workflow.backend.models.Usuario;
import com.workflow.backend.enums.RolUsuario;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends MongoRepository<Usuario, String> {
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRol(RolUsuario rol);
    /** Legacy — búsqueda por campo departamento simple */
    List<Usuario> findByDepartamento(String departamento);
    /** Multi-departamento (CU-03): busca usuarios cuya lista departamentos contenga el valor dado */
    List<Usuario> findByDepartamentosContaining(String departamento);
    boolean existsByEmail(String email);
}