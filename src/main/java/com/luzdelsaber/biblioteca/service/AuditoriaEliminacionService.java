package com.luzdelsaber.biblioteca.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuditoriaEliminacionService {

    private final JdbcTemplate jdbcTemplate;

    public AuditoriaEliminacionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void registrarBaja(
            String entidad,
            Integer idRegistro,
            String descripcionRegistro,
            String estadoAnterior,
            String estadoNuevo,
            Integer idResponsable,
            String nombreResponsable,
            String motivo) {
        registrarMovimiento(
                entidad, idRegistro, descripcionRegistro, estadoAnterior, estadoNuevo,
                "BAJA", idResponsable, nombreResponsable, motivo);
    }

    public void registrarReactivacion(
            String entidad,
            Integer idRegistro,
            String descripcionRegistro,
            String estadoAnterior,
            String estadoNuevo,
            Integer idResponsable,
            String nombreResponsable,
            String motivo) {
        registrarMovimiento(
                entidad, idRegistro, descripcionRegistro, estadoAnterior, estadoNuevo,
                "REACTIVACION", idResponsable, nombreResponsable, motivo);
    }

    private void registrarMovimiento(
            String entidad,
            Integer idRegistro,
            String descripcionRegistro,
            String estadoAnterior,
            String estadoNuevo,
            String tipoMovimiento,
            Integer idResponsable,
            String nombreResponsable,
            String motivo) {
        jdbcTemplate.update("""
                INSERT INTO historial_movimientos_logicos
                    (entidad, id_registro, descripcion_registro, estado_anterior, estado_nuevo,
                     tipo_movimiento, id_usuario_responsable, nombre_responsable, motivo)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                entidad,
                idRegistro,
                descripcionRegistro,
                estadoAnterior,
                estadoNuevo,
                tipoMovimiento,
                idResponsable,
                StringUtils.hasText(nombreResponsable) ? nombreResponsable.trim() : "No disponible",
                StringUtils.hasText(motivo) ? motivo.trim() : "Baja lógica solicitada desde el sistema.");
    }
}
