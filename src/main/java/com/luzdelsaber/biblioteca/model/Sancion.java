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

@Entity
@Table(name = "sancion")
public class Sancion {

    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String TIPO_SUSPENSION_TEMPORAL = "SUSPENSION_TEMPORAL";
    public static final String TIPO_SUSPENSION_TOTAL = "SUSPENSION_TOTAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sancion")
    private Integer idSancion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_incidencia", nullable = false)
    private Incidencia incidencia;

    @Column(name = "tipo_sancion", nullable = false, length = 50)
    private String tipo;

    @Column(name = "dias_suspension")
    private Integer diasSuspension;

    @Column(name = "estado_sancion", nullable = false, length = 20)
    private String estado = ESTADO_ACTIVA;

    public Integer getIdSancion() {
        return idSancion;
    }

    public void setIdSancion(Integer idSancion) {
        this.idSancion = idSancion;
    }

    public Incidencia getIncidencia() {
        return incidencia;
    }

    public void setIncidencia(Incidencia incidencia) {
        this.incidencia = incidencia;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getDiasSuspension() {
        return diasSuspension;
    }

    public void setDiasSuspension(Integer diasSuspension) {
        this.diasSuspension = diasSuspension;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
