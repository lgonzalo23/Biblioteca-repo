package com.luzdelsaber.biblioteca.repository;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.ReservaDetalle;

public interface ReservaDetalleRepository extends JpaRepository<ReservaDetalle, Integer> {

    @Modifying
    @Query(value = """
            UPDATE reserva_detalle
            SET estado_detalle_reserva = :estado
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void actualizarEstadoPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            UPDATE reserva_detalle
            SET fecha_recojo_limite = :fechaRecojoLimite,
                hora_reserva = :horaReserva,
                horas_prestamo = :horasPrestamo,
                hora_recojo_limite = :horaRecojoLimite
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void actualizarFechaRecojoPorReserva(
            @Param("idReserva") Integer idReserva,
            @Param("fechaRecojoLimite") LocalDate fechaRecojoLimite,
            @Param("horaReserva") LocalTime horaReserva,
            @Param("horasPrestamo") Integer horasPrestamo,
            @Param("horaRecojoLimite") LocalTime horaRecojoLimite);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            DELETE FROM reserva_detalle
            WHERE id_reserva = :idReserva
            """, nativeQuery = true)
    void eliminarPorReserva(@Param("idReserva") Integer idReserva);
}
