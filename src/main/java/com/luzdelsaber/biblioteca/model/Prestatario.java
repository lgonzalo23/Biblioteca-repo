package com.luzdelsaber.biblioteca.model;

import java.util.Collections;
import java.util.List;

public class Prestatario extends Usuario {

    private Integer tiempoSuspendido;

    public Reserva solicitarReserva(Libro libro) {
        return new Reserva(libro);
    }

    public boolean solicitarAmpliacion(Prestamo prestamo) {
        return prestamo != null;
    }

    public boolean devolverLibro(Prestamo prestamo) {
        return prestamo != null;
    }

    public List<Prestamo> consultarHistorial() {
        return Collections.emptyList();
    }

    public Integer getTiempoSuspendido() {
        return tiempoSuspendido;
    }

    public void setTiempoSuspendido(Integer tiempoSuspendido) {
        this.tiempoSuspendido = tiempoSuspendido;
    }
}
