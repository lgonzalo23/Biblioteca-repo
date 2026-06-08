package com.luzdelsaber.biblioteca.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreRolIgnoreCase(String nombreRol);

    @Modifying
    @Query(value = """
            INSERT INTO rol
                (nombre_rol)
            VALUES
                (:nombreRol)
            """, nativeQuery = true)
    void insertarRol(@Param("nombreRol") String nombreRol);
}
