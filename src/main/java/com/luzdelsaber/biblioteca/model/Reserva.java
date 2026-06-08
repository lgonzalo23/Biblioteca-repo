package com.luzdelsaber.biblioteca.model;

public class Reserva {

    private Libro libro;

    public Reserva() {
    }

    public Reserva(Libro libro) {
        this.libro = libro;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }
}
