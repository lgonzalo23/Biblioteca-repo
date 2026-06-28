package com.luzdelsaber.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Sancion;

public interface SancionRepository extends JpaRepository<Sancion, Integer> {

    Optional<Sancion> findFirstByIncidenciaIdIncidenciaOrderByIdSancionDesc(Integer idIncidencia);

    boolean existsByIncidenciaIdIncidencia(Integer idIncidencia);

    @Query("""
            SELECT s
            FROM Sancion s
            INNER JOIN s.incidencia i
            INNER JOIN i.prestamo p
            WHERE p.usuario.idUsuario = :idUsuario
              AND s.estado = 'ACTIVA'
            ORDER BY s.idSancion DESC
            """)
    List<Sancion> buscarActivasPorUsuario(@Param("idUsuario") Integer idUsuario);
}
