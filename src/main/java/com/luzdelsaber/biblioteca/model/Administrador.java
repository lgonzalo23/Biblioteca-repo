package com.luzdelsaber.biblioteca.model;

public class Administrador extends Usuario {

    private Integer nivelAcceso;

    public boolean registrarUsuario(Usuario usuario) {
        return usuario != null;
    }

    public boolean eliminarUsuario(Integer idUsuario) {
        return idUsuario != null && idUsuario > 0;
    }

    public Reporte generarReporte(String tipo) {
        return new Reporte(tipo);
    }

    public Integer getNivelAcceso() {
        return nivelAcceso;
    }

    public void setNivelAcceso(Integer nivelAcceso) {
        this.nivelAcceso = nivelAcceso;
    }
}
