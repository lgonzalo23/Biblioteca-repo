package com.luzdelsaber.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AutorForm {

    private Integer idAutor;

    @NotBlank(message = "El nombre del autor es obligatorio.")
    @Size(max = 50, message = "El nombre del autor no puede superar 50 caracteres.")
    private String nombre;

    @NotBlank(message = "El apellido del autor es obligatorio.")
    @Size(max = 50, message = "El apellido del autor no puede superar 50 caracteres.")
    private String apellido;

    @Size(max = 30, message = "La nacionalidad no puede superar 30 caracteres.")
    private String nacionalidad;

    @NotBlank(message = "El estado del autor es obligatorio.")
    @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado del autor debe ser ACTIVO o INACTIVO.")
    private String estado = "ACTIVO";

    public Integer getIdAutor() {
        return idAutor;
    }

    public void setIdAutor(Integer idAutor) {
        this.idAutor = idAutor;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
