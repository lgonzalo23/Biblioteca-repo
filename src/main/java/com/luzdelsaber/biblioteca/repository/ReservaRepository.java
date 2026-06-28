package com.luzdelsaber.biblioteca.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {

    @Modifying
    @Query(value = """
            UPDATE reserva
            SET estado_reserva = :estado
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void actualizarEstado(
            @Param("idReserva") Integer idReserva,
            @Param("estado") String estado);

    @Query(value = """
            SELECT DISTINCT r.*
            FROM reserva r
            INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
            WHERE r.id_usuario = :idUsuario
            ORDER BY r.fecha_reserva DESC, r.id_reserva DESC
            """, nativeQuery = true)
    List<Reserva> listarPorUsuario(@Param("idUsuario") Integer idUsuario);

    @Query(value = """
            SELECT DISTINCT r.*
            FROM reserva r
            INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
            ORDER BY r.fecha_reserva DESC, r.id_reserva DESC
            """, nativeQuery = true)
    List<Reserva> listarTodas();

    @Query(value = """
            SELECT DISTINCT r.*
            FROM reserva r
            INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
            INNER JOIN libro l ON l.id_libro = rd.id_libro
            LEFT JOIN categoria c ON c.id_categoria = l.id_categoria
            LEFT JOIN libro_autor la ON la.id_libro = l.id_libro
            LEFT JOIN autor a ON a.id_autor = la.id_autor
            WHERE r.id_usuario = :idUsuario
              AND (
                    lower(l.titulo_libro) LIKE lower(concat('%', :termino, '%'))
                 OR lower(l.isbn) LIKE lower(concat('%', :termino, '%'))
                 OR lower(l.ubicacion) LIKE lower(concat('%', :termino, '%'))
                 OR lower(r.estado_reserva) LIKE lower(concat('%', :termino, '%'))
                 OR lower(c.nombre_categoria) LIKE lower(concat('%', :termino, '%'))
                 OR lower(a.nombre_autor) LIKE lower(concat('%', :termino, '%'))
                 OR lower(a.apellido_autor) LIKE lower(concat('%', :termino, '%'))
              )
            ORDER BY r.fecha_reserva DESC, r.id_reserva DESC
            """, nativeQuery = true)
    List<Reserva> buscarPorUsuario(
            @Param("idUsuario") Integer idUsuario,
            @Param("termino") String termino);

    @Query(value = """
            SELECT DISTINCT r.*
            FROM reserva r
            INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
            INNER JOIN libro l ON l.id_libro = rd.id_libro
            INNER JOIN usuario u ON u.id_usuario = r.id_usuario
            LEFT JOIN categoria c ON c.id_categoria = l.id_categoria
            LEFT JOIN libro_autor la ON la.id_libro = l.id_libro
            LEFT JOIN autor a ON a.id_autor = la.id_autor
            WHERE lower(l.titulo_libro) LIKE lower(concat('%', :termino, '%'))
               OR lower(l.isbn) LIKE lower(concat('%', :termino, '%'))
               OR lower(l.ubicacion) LIKE lower(concat('%', :termino, '%'))
               OR lower(r.estado_reserva) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.nombre_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.apellido_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.dni_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.email_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(c.nombre_categoria) LIKE lower(concat('%', :termino, '%'))
               OR lower(a.nombre_autor) LIKE lower(concat('%', :termino, '%'))
               OR lower(a.apellido_autor) LIKE lower(concat('%', :termino, '%'))
            ORDER BY r.fecha_reserva DESC, r.id_reserva DESC
            """, nativeQuery = true)
    List<Reserva> buscarTodas(@Param("termino") String termino);

    @Query(value = """
            SELECT count(*)
            FROM reserva r
            INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
            WHERE r.id_usuario = :idUsuario
              AND rd.id_libro = :idLibro
              AND r.estado_reserva = 'ACTIVA'
              AND rd.estado_detalle_reserva = 'ACTIVA'
            """, nativeQuery = true)
    Integer contarActivasPorUsuarioYLibro(
            @Param("idUsuario") Integer idUsuario,
            @Param("idLibro") Integer idLibro);

    @Query(value = """
            SELECT r.*
            FROM reserva r
            WHERE r.id_reserva = :idReserva
              AND r.id_usuario = :idUsuario
            """, nativeQuery = true)
    Optional<Reserva> buscarDelUsuario(
            @Param("idReserva") Integer idReserva,
            @Param("idUsuario") Integer idUsuario);

    @Modifying
    @Query(value = """
            UPDATE reserva
            SET fecha_reserva = :fechaReserva
            WHERE id_reserva = :idReserva
              AND id_usuario = :idUsuario
              AND estado_reserva = 'ACTIVA'
            """, nativeQuery = true)
    int actualizarFechaReserva(
            @Param("idReserva") Integer idReserva,
            @Param("idUsuario") Integer idUsuario,
            @Param("fechaReserva") LocalDate fechaReserva);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM reserva
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void eliminarFisico(@Param("idReserva") Integer idReserva);
}
