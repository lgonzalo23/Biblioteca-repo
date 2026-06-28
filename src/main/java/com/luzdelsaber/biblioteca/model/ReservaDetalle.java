package com.luzdelsaber.biblioteca.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva_detalle")
public class ReservaDetalle {

    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_CANCELADA = "CANCELADA";
    public static final String ESTADO_PRESTADA = "PRESTADA";
    public static final String ESTADO_DEVUELTA = "DEVUELTA";
    public static final String ESTADO_VENCIDA = "VENCIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_reserva", nullable = false)
    private Reserva reserva;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @Column(name = "fecha_recojo_limite", nullable = false)
    private LocalDate fechaRecojoLimite;

    @Column(name = "hora_reserva", nullable = false)
    private LocalTime horaReserva;

    @Column(name = "horas_prestamo", nullable = false)
    private Integer horasPrestamo = 1;

    @Column(name = "hora_recojo_limite", nullable = false)
    private LocalTime horaRecojoLimite;

    @Column(name = "estado_detalle_reserva", nullable = false, length = 20)
    private String estadoDetalleReserva = ESTADO_ACTIVA;

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public LocalDate getFechaRecojoLimite() {
        return fechaRecojoLimite;
    }

    public void setFechaRecojoLimite(LocalDate fechaRecojoLimite) {
        this.fechaRecojoLimite = fechaRecojoLimite;
    }

    public LocalTime getHoraReserva() {
        return horaReserva;
    }

    public void setHoraReserva(LocalTime horaReserva) {
        this.horaReserva = horaReserva;
    }

    public Integer getHorasPrestamo() {
        return horasPrestamo;
    }

    public void setHorasPrestamo(Integer horasPrestamo) {
        this.horasPrestamo = horasPrestamo;
    }

    public LocalTime getHoraRecojoLimite() {
        return horaRecojoLimite;
    }

    public void setHoraRecojoLimite(LocalTime horaRecojoLimite) {
        this.horaRecojoLimite = horaRecojoLimite;
    }

    public String getEstadoDetalleReserva() {
        return estadoDetalleReserva;
    }

    public void setEstadoDetalleReserva(String estadoDetalleReserva) {
        this.estadoDetalleReserva = estadoDetalleReserva;
    }
}
