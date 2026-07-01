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
import jakarta.persistence.Transient;

@Entity
@Table(name = "prestamo")
public class Prestamo {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_CANCELADO = "CANCELADO";
    public static final String ESTADO_DEVUELTO = "DEVUELTO";
    public static final String ESTADO_DEVUELTO_TARDIO = "DEVUELTO_TARDIO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prestamo")
    private Integer idPrestamo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_libro", nullable = false)
    private Libro libro;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_reserva")
    private Reserva reserva;

    @Column(name = "fecha_prestamo", nullable = false)
    private LocalDate fechaPrestamo;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "fecha_devolucion_real")
    private LocalDate fechaDevolucionReal;

    @Column(name = "hora_devolucion_real")
    private LocalTime horaDevolucionReal;

    @Column(name = "estado_prestamo", nullable = false, length = 20)
    private String estado = ESTADO_ACTIVO;

    @Column(name = "incidencia_revisada", nullable = false)
    private boolean incidenciaRevisada;

    @Transient
    private boolean incidenciaRegistrada;

    public Integer getIdPrestamo() {
        return idPrestamo;
    }

    public void setIdPrestamo(Integer idPrestamo) {
        this.idPrestamo = idPrestamo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public void setReserva(Reserva reserva) {
        this.reserva = reserva;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public LocalDate getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDate fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public LocalTime getHoraDevolucionReal() {
        return horaDevolucionReal;
    }

    public void setHoraDevolucionReal(LocalTime horaDevolucionReal) {
        this.horaDevolucionReal = horaDevolucionReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isIncidenciaRevisada() {
        return incidenciaRevisada;
    }

    public void setIncidenciaRevisada(boolean incidenciaRevisada) {
        this.incidenciaRevisada = incidenciaRevisada;
    }

    public boolean isIncidenciaRegistrada() {
        return incidenciaRegistrada;
    }

    public void setIncidenciaRegistrada(boolean incidenciaRegistrada) {
        this.incidenciaRegistrada = incidenciaRegistrada;
    }
}
