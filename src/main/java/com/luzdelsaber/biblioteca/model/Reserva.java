package com.luzdelsaber.biblioteca.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_CANCELADA = "CANCELADA";
    public static final String ESTADO_PRESTADA = "PRESTADA";
    public static final String ESTADO_DEVUELTA = "DEVUELTA";
    public static final String ESTADO_VENCIDA = "VENCIDA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Integer idReserva;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "estado_reserva", nullable = false, length = 20)
    private String estado = ESTADO_ACTIVA;

    @OneToMany(mappedBy = "reserva", fetch = FetchType.EAGER)
    private List<ReservaDetalle> detalles = new ArrayList<>();

    @Transient
    private List<Prestamo> prestamos = new ArrayList<>();

    @Transient
    private String tiempoRestantePrestamo;

    @Transient
    private boolean disponibleParaPrestar;

    @Transient
    private String estadoGestion;

    public Reserva() {
    }

    public Reserva(Libro libro) {
        ReservaDetalle detalle = new ReservaDetalle();
        detalle.setReserva(this);
        detalle.setLibro(libro);
        this.detalles = List.of(detalle);
        this.estado = ESTADO_ACTIVA;
    }

    public Reserva(Usuario usuario, LocalDate fechaReserva) {
        this.usuario = usuario;
        this.fechaReserva = fechaReserva;
        this.estado = ESTADO_ACTIVA;
    }

    public boolean estaActiva() {
        return ESTADO_ACTIVA.equals(estado);
    }

    public Integer getIdReserva() {
        return idReserva;
    }

    public void setIdReserva(Integer idReserva) {
        this.idReserva = idReserva;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDate fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public LocalTime getHoraReserva() {
        ReservaDetalle detalle = getDetallePrincipal();
        return detalle == null ? null : detalle.getHoraReserva();
    }

    public void setHoraReserva(LocalTime horaReserva) {
        ReservaDetalle detalle = getDetallePrincipal();
        if (detalle != null) {
            detalle.setHoraReserva(horaReserva);
        }
    }

    public Integer getHorasPrestamo() {
        ReservaDetalle detalle = getDetallePrincipal();
        return detalle == null ? null : detalle.getHorasPrestamo();
    }

    public void setHorasPrestamo(Integer horasPrestamo) {
        ReservaDetalle detalle = getDetallePrincipal();
        if (detalle != null) {
            detalle.setHorasPrestamo(horasPrestamo);
        }
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<ReservaDetalle> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<ReservaDetalle> detalles) {
        this.detalles = detalles;
    }

    public List<Prestamo> getPrestamos() {
        return prestamos;
    }

    public void setPrestamos(List<Prestamo> prestamos) {
        this.prestamos = prestamos;
    }

    public String getTiempoRestantePrestamo() {
        return tiempoRestantePrestamo;
    }

    public void setTiempoRestantePrestamo(String tiempoRestantePrestamo) {
        this.tiempoRestantePrestamo = tiempoRestantePrestamo;
    }

    public boolean isDisponibleParaPrestar() {
        return disponibleParaPrestar;
    }

    public void setDisponibleParaPrestar(boolean disponibleParaPrestar) {
        this.disponibleParaPrestar = disponibleParaPrestar;
    }

    public String getEstadoGestion() {
        return estadoGestion;
    }

    public void setEstadoGestion(String estadoGestion) {
        this.estadoGestion = estadoGestion;
    }

    public ReservaDetalle getDetallePrincipal() {
        return detalles == null || detalles.isEmpty() ? null : detalles.get(0);
    }

    public List<Integer> getIdsLibro() {
        if (detalles == null) {
            return List.of();
        }
        return detalles.stream()
                .filter(detalle -> detalle.getLibro() != null)
                .map(detalle -> detalle.getLibro().getIdLibro())
                .toList();
    }

    public boolean contieneLibro(Integer idLibro) {
        return idLibro != null && getIdsLibro().contains(idLibro);
    }

    public Libro getLibro() {
        ReservaDetalle detalle = getDetallePrincipal();
        return detalle == null ? null : detalle.getLibro();
    }

    public LocalDate getFechaRecojoLimite() {
        ReservaDetalle detalle = getDetallePrincipal();
        return detalle == null ? null : detalle.getFechaRecojoLimite();
    }

    public LocalTime getHoraRecojoLimite() {
        ReservaDetalle detalle = getDetallePrincipal();
        return detalle == null ? null : detalle.getHoraRecojoLimite();
    }

    public LocalTime getHoraDevolucionPrestamo() {
        if (prestamos == null || prestamos.isEmpty()) {
            return null;
        }
        return prestamos.get(0).getHoraFin();
    }

    public LocalDate getFechaDevolucionRealPrestamo() {
        if (prestamos == null || prestamos.isEmpty()) {
            return null;
        }
        return prestamos.get(0).getFechaDevolucionReal();
    }

    public LocalTime getHoraDevolucionRealPrestamo() {
        if (prestamos == null || prestamos.isEmpty()) {
            return null;
        }
        return prestamos.get(0).getHoraDevolucionReal();
    }

    public String getEstadoPrestamoPrincipal() {
        if (prestamos == null || prestamos.isEmpty()) {
            return null;
        }
        return prestamos.get(0).getEstado();
    }

    public boolean isTienePrestamosDevueltosSinIncidencia() {
        return prestamos != null && prestamos.stream()
                .anyMatch(prestamo -> (Prestamo.ESTADO_DEVUELTO.equals(prestamo.getEstado())
                        || Prestamo.ESTADO_DEVUELTO_TARDIO.equals(prestamo.getEstado()))
                        && !prestamo.isIncidenciaRegistrada());
    }
}
