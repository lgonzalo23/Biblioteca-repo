package com.luzdelsaber.biblioteca.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LibroForm {

    private Integer idLibro;

    @NotNull(message = "Debe seleccionar una categoria.")
    private Integer categoriaId;

    @NotBlank(message = "El titulo del libro es obligatorio.")
    @Size(max = 150, message = "El titulo no puede superar 150 caracteres.")
    private String titulo;

    @NotBlank(message = "El ISBN es obligatorio.")
    @Size(max = 20, message = "El ISBN no puede superar 20 caracteres.")
    private String isbn;

    private LocalDate fechaPublicacion;

    @NotBlank(message = "El estado es obligatorio.")
    @Pattern(regexp = "DISPONIBLE|PRESTADO|RESERVADO|NO DISPONIBLE",
            message = "El estado debe ser Disponible, Prestado, Reservado o No disponible.")
    private String estado = "DISPONIBLE";

    @NotNull(message = "El stock es obligatorio.")
    @Min(value = 0, message = "El stock debe ser mayor o igual a 0.")
    private Integer stock;

    @NotBlank(message = "La ubicacion es obligatoria.")
    @Size(max = 50, message = "La ubicacion no puede superar 50 caracteres.")
    private String ubicacion;

    @Size(max = 255, message = "El enlace de imagen no puede superar 255 caracteres.")
    private String urlImagen;

    @NotEmpty(message = "Debe seleccionar al menos un autor.")
    private List<Integer> autorIds = new ArrayList<>();

    public Integer getIdLibro() {
        return idLibro;
    }

    public void setIdLibro(Integer idLibro) {
        this.idLibro = idLibro;
    }

    public Integer getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Integer categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public LocalDate getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDate fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public void setUrlImagen(String urlImagen) {
        this.urlImagen = urlImagen;
    }

    public List<Integer> getAutorIds() {
        return autorIds;
    }

    public void setAutorIds(List<Integer> autorIds) {
        this.autorIds = autorIds;
    }
}
