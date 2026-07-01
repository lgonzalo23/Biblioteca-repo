package com.luzdelsaber.biblioteca.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Prestamo;

public interface PrestamoRepository extends JpaRepository<Prestamo, Integer> {

    List<Prestamo> findByReservaIdReservaOrderByIdPrestamoAsc(Integer idReserva);

    List<Prestamo> findByReservaIdReservaAndEstadoOrderByIdPrestamoAsc(Integer idReserva, String estado);

    @Modifying
    @Query(value = """
            UPDATE prestamo
            SET estado_prestamo = :estado
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void actualizarEstadoPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            UPDATE prestamo
            SET estado_prestamo = :estado,
                fecha_devolucion_real = :fechaDevolucionReal,
                hora_devolucion_real = :horaDevolucionReal
            WHERE id_reserva = :idReserva
              AND estado_prestamo = 'ACTIVO'
            """, nativeQuery = true)
    int registrarDevolucionPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("estado") String estado,
            @Param("fechaDevolucionReal") LocalDate fechaDevolucionReal,
            @Param("horaDevolucionReal") LocalTime horaDevolucionReal);

    @Modifying
    @Query(value = """
            UPDATE prestamo
            SET incidencia_revisada = true
            WHERE id_prestamo = :idPrestamo
            """, nativeQuery = true)
    int marcarIncidenciaRevisada(@Param("idPrestamo") Integer idPrestamo);

    @Modifying
    @Query(value = """
            UPDATE prestamo
            SET hora_fin = :horaFin
            WHERE id_reserva = :idReserva
              AND estado_prestamo = 'ACTIVO'
            """, nativeQuery = true)
    int postergarPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("horaFin") LocalTime horaFin);

    @Modifying
    @Query(value = """
            UPDATE prestamo
            SET hora_fin = :horaFin
            WHERE id_reserva = :idReserva
              AND estado_prestamo = 'ACTIVO'
            """, nativeQuery = true)
    int ampliarPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("horaFin") LocalTime horaFin);
}
