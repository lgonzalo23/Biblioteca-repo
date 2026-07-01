package com.luzdelsaber.biblioteca.model;

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
@Table(name = "usuario")
public class Usuario {

    public static final String ESTADO_ACTIVO = "ACTIVO";
    public static final String ESTADO_INACTIVO = "INACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre_usuario", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido_usuario", nullable = false, length = 50)
    private String apellido;

    @Column(name = "dni_usuario", nullable = false, unique = true, length = 8)
    private String dni;

    @Column(name = "email_usuario", nullable = false, unique = true, length = 50)
    private String correo;

    @Column(name = "contraseña_usuario", nullable = false, length = 50)
    private String contrasena;

    @Column(name = "estado_usuario", nullable = false, length = 20)
    private String estado;

    @Transient
    private boolean reactivable;

    public boolean iniciarSesion(String correo, String contrasena) {
        return ESTADO_ACTIVO.equals(estado)
                && this.correo != null
                && this.correo.equalsIgnoreCase(correo)
                && this.contrasena != null
                && this.contrasena.equals(contrasena);
    }

    public void cerrarSesion() {
        // La sesion HTTP se cierra desde el controlador.
    }

    public void actualizarDatos(String nombre, String apellido, String correo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
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

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isReactivable() {
        return reactivable;
    }

    public void setReactivable(boolean reactivable) {
        this.reactivable = reactivable;
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
