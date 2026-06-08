package com.luzdelsaber.biblioteca.model;

public class Encargado extends Usuario {

    private String turno;

    public boolean aprobarPrestamo(Prestamo prestamo) {
        return prestamo != null;
    }

    public boolean registrarDevolucion(Prestamo prestamo) {
        return prestamo != null;
    }

    public boolean aplicarSancion(Usuario usuario, Sancion sancion) {
        return usuario != null && sancion != null;
    }

    public boolean aprobarAmpliacion(Prestamo prestamo) {
        return prestamo != null;
    }

    public String getTurno() {
        return turno;
    }

    public void setTurno(String turno) {
        this.turno = turno;
    }
}
