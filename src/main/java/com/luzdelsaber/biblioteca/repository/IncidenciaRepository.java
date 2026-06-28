package com.luzdelsaber.biblioteca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Incidencia;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer> {

    List<Incidencia> findAllByOrderByFechaIncidenciaDescIdIncidenciaDesc();

    @Query("""
            SELECT i
            FROM Incidencia i
            INNER JOIN i.prestamo p
            INNER JOIN p.usuario u
            WHERE lower(i.tipo) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.nombre) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.apellido) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.correo) LIKE lower(concat('%', :termino, '%'))
               OR lower(concat(u.nombre, ' ', u.apellido)) LIKE lower(concat('%', :termino, '%'))
            ORDER BY i.fechaIncidencia DESC, i.idIncidencia DESC
            """)
    List<Incidencia> buscarPorUsuarioOTipo(@Param("termino") String termino);

    boolean existsByPrestamoIdPrestamo(Integer idPrestamo);

    @Query(value = """
            SELECT COUNT(DISTINCT COALESCE(p.id_reserva, p.id_prestamo))
            FROM incidencia i
            INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
            WHERE p.id_usuario = :idUsuario
              AND i.tipo_incidencia = 'RETRASO'
              AND i.fecha_incidencia >= DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01')
              AND i.fecha_incidencia < DATE_ADD(DATE_FORMAT(CURRENT_DATE(), '%Y-%m-01'), INTERVAL 1 MONTH)
            """, nativeQuery = true)
    long contarRetrasosDelMesPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query(value = """
            SELECT COUNT(*)
            FROM incidencia i
            INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
            WHERE p.id_reserva = :idReserva
              AND i.tipo_incidencia = 'RETRASO'
            """, nativeQuery = true)
    long contarRetrasosPorReserva(@Param("idReserva") Integer idReserva);
}
