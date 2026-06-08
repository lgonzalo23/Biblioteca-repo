package com.luzdelsaber.biblioteca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CategoriaForm {

    private Integer idCategoria;

    @NotBlank(message = "El nombre de la categoria es obligatorio.")
    @Size(max = 50, message = "El nombre de la categoria no puede superar 50 caracteres.")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s.'-]+$",
            message = "El nombre de la categoria solo puede contener letras.")
    private String nombre;

    @Size(max = 150, message = "La descripcion de la categoria no puede superar 150 caracteres.")
    private String descripcion;

    @NotBlank(message = "El estado de la categoria es obligatorio.")
    @Pattern(regexp = "ACTIVO|INACTIVO", message = "El estado de la categoria debe ser ACTIVO o INACTIVO.")
    private String estado = "ACTIVO";

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
