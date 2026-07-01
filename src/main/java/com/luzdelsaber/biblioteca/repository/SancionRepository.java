package com.luzdelsaber.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(value = """
            UPDATE sancion s
            INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
            INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
            SET s.estado_sancion = 'INACTIVA'
            WHERE p.id_usuario = :idUsuario
              AND s.estado_sancion = 'ACTIVA'
              AND s.tipo_sancion = 'SUSPENSION_TEMPORAL'
              AND s.dias_suspension IS NOT NULL
              AND DATE_ADD(i.fecha_registro, INTERVAL s.dias_suspension DAY) <= CURRENT_DATE
            """, nativeQuery = true)
    int desactivarTemporalesVencidasPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Modifying
    @Query(value = """
            UPDATE sancion s
            INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
            INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
            SET s.estado_sancion = :estado
            WHERE p.id_usuario = :idUsuario
              AND s.estado_sancion = 'ACTIVA'
            """, nativeQuery = true)
    int actualizarActivasPorUsuario(
            @Param("idUsuario") Integer idUsuario,
            @Param("estado") String estado);
}
