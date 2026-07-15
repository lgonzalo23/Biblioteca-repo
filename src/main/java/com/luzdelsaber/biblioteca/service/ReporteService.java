package com.luzdelsaber.biblioteca.service;

import java.io.InputStream;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

@Service
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd-MM-yy");

    private final JdbcTemplate jdbcTemplate;

    public ReporteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generarReporteTiempoPromedio(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Integer totalPrestamos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM prestamo
                    WHERE MONTH(fecha_devolucion_real) = ?
                      AND YEAR(fecha_devolucion_real) = ?
                      AND fecha_devolucion_real IS NOT NULL
                      AND hora_devolucion_real IS NOT NULL
                      AND estado_prestamo IN ('DEVUELTO', 'DEVUELTO_TARDIO')
                    """, Integer.class, periodo.mes(), periodo.anio());

            Long sumatoriaMinutos = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(SUM(GREATEST(
                               TIMESTAMPDIFF(
                                   MINUTE,
                                   TIMESTAMP(fecha_prestamo, hora_inicio),
                                   TIMESTAMP(fecha_devolucion_real, hora_devolucion_real)
                               ),
                               0
                           )), 0)
                    FROM prestamo
                    WHERE MONTH(fecha_devolucion_real) = ?
                      AND YEAR(fecha_devolucion_real) = ?
                      AND fecha_devolucion_real IS NOT NULL
                      AND hora_devolucion_real IS NOT NULL
                      AND estado_prestamo IN ('DEVUELTO', 'DEVUELTO_TARDIO')
                    """, Long.class, periodo.mes(), periodo.anio());

            Double tiempoPromedio = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(AVG(GREATEST(
                               TIMESTAMPDIFF(
                                   MINUTE,
                                   TIMESTAMP(fecha_prestamo, hora_inicio),
                                   TIMESTAMP(fecha_devolucion_real, hora_devolucion_real)
                               ),
                               0
                           )), 0.0)
                    FROM prestamo
                    WHERE MONTH(fecha_devolucion_real) = ?
                      AND YEAR(fecha_devolucion_real) = ?
                      AND fecha_devolucion_real IS NOT NULL
                      AND hora_devolucion_real IS NOT NULL
                      AND estado_prestamo IN ('DEVUELTO', 'DEVUELTO_TARDIO')
                    """, Double.class, periodo.mes(), periodo.anio());

            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("totalPrestamos", valorOZero(totalPrestamos));
            parametros.put("sumatoriaMinutos", valorOZero(sumatoriaMinutos));
            parametros.put("tiempoPromedio", redondearUnaCifra(valorOZero(tiempoPromedio)));

            return generarPdf(
                    "/reports/reporte_tiempo_promedio.jrxml",
                    parametros,
                    new JREmptyDataSource());
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de tiempo promedio.", ex);
        }
    }

    public byte[] generarReporteLibrosMasSolicitados(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Integer totalLibrosUnicos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(DISTINCT id_libro)
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    """, Integer.class, periodo.mes(), periodo.anio());

            String categoriaMasLeida = obtenerCategoriaMasLeida(periodo);

            List<Map<String, Object>> libros = jdbcTemplate.queryForList("""
                    SELECT l.titulo_libro AS titulo,
                           COALESCE(
                               GROUP_CONCAT(
                                   DISTINCT CONCAT(a.nombre_autor, ' ', a.apellido_autor)
                                   ORDER BY a.apellido_autor, a.nombre_autor SEPARATOR ', '
                               ),
                               'Autor Desconocido'
                           ) AS autor,
                           c.nombre_categoria AS categoria,
                           COUNT(DISTINCT p.id_prestamo) AS cantidad
                    FROM prestamo p
                    INNER JOIN libro l ON p.id_libro = l.id_libro
                    INNER JOIN categoria c ON l.id_categoria = c.id_categoria
                    LEFT JOIN libro_autor la ON l.id_libro = la.id_libro
                    LEFT JOIN autor a ON la.id_autor = a.id_autor
                    WHERE MONTH(p.fecha_prestamo) = ?
                      AND YEAR(p.fecha_prestamo) = ?
                      AND p.estado_prestamo <> 'CANCELADO'
                    GROUP BY l.id_libro, l.titulo_libro, c.nombre_categoria
                    ORDER BY cantidad DESC, l.titulo_libro ASC
                    """, periodo.mes(), periodo.anio());

            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("totalLibrosUnicos", valorOZero(totalLibrosUnicos));
            parametros.put("categoriaMasLeida", categoriaMasLeida);
            parametros.put("libroMasSolicitado", libros.isEmpty()
                    ? "Sin datos"
                    : String.valueOf(libros.get(0).get("titulo")));

            return generarPdf(
                    "/reports/reporte_libros_mas_solicitados.jrxml",
                    parametros,
                    crearDataSource(libros));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de libros más solicitados.", ex);
        }
    }

    public byte[] generarReporteHorarioMayorDemanda(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Integer totalPrestamos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    """, Integer.class, periodo.mes(), periodo.anio());
            int total = valorOZero(totalPrestamos);

            List<Map<String, Object>> horas = jdbcTemplate.queryForList("""
                    SELECT HOUR(hora_inicio) AS hora_num,
                           COUNT(*) AS cantidad_prestamos
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    GROUP BY HOUR(hora_inicio)
                    ORDER BY HOUR(hora_inicio)
                    """, periodo.mes(), periodo.anio());

            String horarioPico = "N/A";
            long cantidadPico = 0L;
            List<Map<String, Object>> filas = new ArrayList<>();

            for (Map<String, Object> hora : horas) {
                int horaInicio = ((Number) hora.get("hora_num")).intValue();
                long cantidad = ((Number) hora.get("cantidad_prestamos")).longValue();
                String rango = String.format("%02d:00 - %02d:00", horaInicio, horaInicio + 1);

                if (cantidad > cantidadPico) {
                    cantidadPico = cantidad;
                    horarioPico = rango;
                }

                Map<String, Object> fila = new HashMap<>();
                fila.put("rango_hora", rango);
                fila.put("cantidad", cantidad);
                fila.put("porcentaje", redondearUnaCifra(total == 0 ? 0.0 : cantidad * 100.0 / total));
                filas.add(fila);
            }

            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("totalPrestamos", total);
            parametros.put("horarioPico", horarioPico);
            parametros.put("cantidadPico", cantidadPico);

            return generarPdf(
                    "/reports/reporte_horario_demanda.jrxml",
                    parametros,
                    crearDataSource(filas));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de horario de mayor demanda.", ex);
        }
    }

    public byte[] generarReportePorcentajeDevolucionesTardias(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Map<String, Object> resumen = jdbcTemplate.queryForMap("""
                    SELECT COUNT(*) AS total_prestamos,
                           COALESCE(SUM(
                               CASE
                                   WHEN estado_prestamo = 'DEVUELTO_TARDIO'
                                     OR TIMESTAMP(fecha_devolucion_real, hora_devolucion_real)
                                        > TIMESTAMP(fecha_prestamo, hora_fin)
                                   THEN 1 ELSE 0
                               END
                           ), 0) AS total_devoluciones_tardias
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    """, periodo.mes(), periodo.anio());

            long totalPrestamos = numero(resumen.get("total_prestamos"));
            long totalDevolucionesTardias = numero(resumen.get("total_devoluciones_tardias"));
            long prestamosSinDevolucionTardia = Math.max(0L, totalPrestamos - totalDevolucionesTardias);
            double porcentaje = totalPrestamos == 0
                    ? 0.0
                    : redondearUnaCifra(totalDevolucionesTardias * 100.0 / totalPrestamos);

            return generarIndicadorResumen(
                    "/reports/reporte_devoluciones_tardias.jrxml",
                    periodo,
                    "Porcentaje de Devoluciones Tardías",
                    "Porcentaje de Devoluciones Tardías",
                    porcentaje + "%",
                    "Número de préstamos mensuales", String.valueOf(totalPrestamos),
                    "Número de devoluciones tardías mensuales", String.valueOf(totalDevolucionesTardias),
                    "Préstamos sin devolución tardía", String.valueOf(prestamosSinDevolucionTardia),
                    "Fórmula: número de devoluciones tardías mensuales / número de préstamos mensuales x 100.");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de devoluciones tardías.", ex);
        }
    }

    public byte[] generarReporteTasaIncidencias(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Integer totalPrestamos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    """, Integer.class, periodo.mes(), periodo.anio());

            Map<String, Object> resumenIncidencias = jdbcTemplate.queryForMap("""
                    SELECT COUNT(*) AS total_incidencias,
                           COUNT(DISTINCT i.id_prestamo) AS prestamos_afectados
                    FROM incidencia i
                    INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
                    WHERE MONTH(p.fecha_prestamo) = ?
                      AND YEAR(p.fecha_prestamo) = ?
                    """, periodo.mes(), periodo.anio());

            long prestamos = valorOZero(totalPrestamos);
            long incidencias = numero(resumenIncidencias.get("total_incidencias"));
            long prestamosAfectados = numero(resumenIncidencias.get("prestamos_afectados"));
            double tasa = prestamos == 0 ? 0.0 : redondearUnaCifra(incidencias * 100.0 / prestamos);

            return generarIndicadorResumen(
                    "/reports/reporte_incidencias_mensuales.jrxml",
                    periodo,
                    "Porcentaje de Incidencias Mensuales",
                    "Porcentaje de Incidencias Mensuales",
                    tasa + "%",
                    "Número de préstamos mensuales", String.valueOf(prestamos),
                    "Número de incidencias mensuales", String.valueOf(incidencias),
                    "Préstamos con incidencias", String.valueOf(prestamosAfectados),
                    "Fórmula: número de incidencias mensuales / número de préstamos mensuales x 100.");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de tasa de incidencias.", ex);
        }
    }

    public byte[] generarReportePromedioPrestamosPorUsuario(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Map<String, Object> resumen = jdbcTemplate.queryForMap("""
                    SELECT COUNT(*) AS total_prestamos
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    """, periodo.mes(), periodo.anio());

            Integer totalUsuariosActivos = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM usuario u
                    INNER JOIN rol r ON r.id_rol = u.id_rol
                    WHERE u.estado_usuario = 'ACTIVO'
                      AND UPPER(r.nombre_rol) = 'PRESTATARIO'
                    """, Integer.class);

            long totalPrestamos = numero(resumen.get("total_prestamos"));
            long usuariosActivos = valorOZero(totalUsuariosActivos);
            double promedio = usuariosActivos == 0
                    ? 0.0
                    : redondearUnaCifra(totalPrestamos * 1.0 / usuariosActivos);

            return generarIndicadorResumen(
                    "/reports/reporte_promedio_prestamos_usuario.jrxml",
                    periodo,
                    "Promedio de Préstamos por Usuario",
                    "Promedio de Préstamos por Usuario",
                    promedio + " préstamos",
                    "Número total de préstamos", String.valueOf(totalPrestamos),
                    "Número total de usuarios activos", String.valueOf(usuariosActivos),
                    "", "",
                    "Fórmula: número total de préstamos / número total de usuarios activos.");
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de promedio por usuario.", ex);
        }
    }

    public byte[] generarReporteEstadisticasPrestamos(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            List<Map<String, Object>> resultados = jdbcTemplate.queryForList("""
                    SELECT DATE_FORMAT(fecha_prestamo, '%d/%m/%Y') AS fecha,
                           COUNT(*) AS cantidad
                    FROM prestamo
                    WHERE MONTH(fecha_prestamo) = ?
                      AND YEAR(fecha_prestamo) = ?
                      AND estado_prestamo <> 'CANCELADO'
                    GROUP BY fecha_prestamo
                    ORDER BY fecha_prestamo
                    """, periodo.mes(), periodo.anio());

            List<Map<String, Object>> filas = new ArrayList<>();
            long totalPrestamos = 0L;
            long maximo = 0L;
            long minimo = 0L;
            String fechaMaximo = "Sin actividad";
            String fechaMinimo = "Sin actividad";

            for (Map<String, Object> resultado : resultados) {
                String fecha = String.valueOf(resultado.get("fecha"));
                long cantidad = numero(resultado.get("cantidad"));

                Map<String, Object> fila = new HashMap<>();
                fila.put("fecha", fecha);
                fila.put("cantidad", cantidad);
                filas.add(fila);

                totalPrestamos += cantidad;
                if (fechaMaximo.equals("Sin actividad") || cantidad > maximo) {
                    maximo = cantidad;
                    fechaMaximo = fecha;
                }
                if (fechaMinimo.equals("Sin actividad") || cantidad < minimo) {
                    minimo = cantidad;
                    fechaMinimo = fecha;
                }
            }

            double promedio = filas.isEmpty() ? 0.0 : redondearUnaCifra(totalPrestamos * 1.0 / filas.size());
            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("totalPrestamos", String.valueOf(totalPrestamos));
            parametros.put("diasActividad", String.valueOf(filas.size()));
            parametros.put("maximo", maximo + " - " + fechaMaximo);
            parametros.put("minimo", minimo + " - " + fechaMinimo);
            parametros.put("promedio", promedio + " préstamos por día con actividad");

            return generarPdf(
                    "/reports/reporte_estadisticas_prestamos.jrxml",
                    parametros,
                    crearDataSource(filas));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte estadístico de préstamos.", ex);
        }
    }

    public byte[] generarReportePrestamosPorRango(LocalDate fechaInicio, LocalDate fechaFin) {
        validarRangoFechas(fechaInicio, fechaFin);

        try {
            List<Map<String, Object>> prestamos = jdbcTemplate.queryForList("""
                    SELECT CAST(p.id_prestamo AS CHAR) AS id_prestamo,
                           CONCAT(DATE_FORMAT(p.fecha_prestamo, '%d/%m/%Y'),
                                  ' ', DATE_FORMAT(p.hora_inicio, '%H:%i')) AS fecha_hora,
                           CONCAT(TRIM(u.nombre_usuario), ' ', TRIM(u.apellido_usuario)) AS usuario,
                           u.dni_usuario AS dni,
                           l.titulo_libro AS libro,
                           p.estado_prestamo AS estado_codigo
                    FROM prestamo p
                    INNER JOIN usuario u ON p.id_usuario = u.id_usuario
                    INNER JOIN libro l ON p.id_libro = l.id_libro
                    WHERE p.fecha_prestamo BETWEEN ? AND ?
                    ORDER BY p.fecha_prestamo, p.hora_inicio, p.id_prestamo
                    """, fechaInicio, fechaFin);

            long activos = 0L;
            long devueltos = 0L;
            long tardios = 0L;
            long cancelados = 0L;

            for (Map<String, Object> prestamo : prestamos) {
                String estado = String.valueOf(prestamo.get("estado_codigo"));
                switch (estado) {
                    case "ACTIVO" -> activos++;
                    case "DEVUELTO" -> devueltos++;
                    case "DEVUELTO_TARDIO" -> tardios++;
                    case "CANCELADO" -> cancelados++;
                    default -> {
                        // El estado se conserva en el detalle aunque no forme parte del resumen conocido.
                    }
                }
                prestamo.put("estado", traducirEstadoPrestamo(estado));
            }

            Map<String, Object> parametros = parametrosRango(fechaInicio, fechaFin);
            parametros.put("totalPrestamos", String.valueOf(prestamos.size()));
            parametros.put("totalActivos", String.valueOf(activos));
            parametros.put("totalDevueltos", String.valueOf(devueltos));
            parametros.put("totalTardios", String.valueOf(tardios));
            parametros.put("totalCancelados", String.valueOf(cancelados));

            return generarPdf(
                    "/reports/reporte_prestamos_rango.jrxml",
                    parametros,
                    crearDataSource(prestamos));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de préstamos por rango de fechas.", ex);
        }
    }

    public byte[] generarComprobantePrestamo(Integer idReserva) {
        if (idReserva == null || idReserva <= 0) {
            throw new IllegalArgumentException("El préstamo seleccionado no es válido.");
        }

        try {
            List<Map<String, Object>> prestamos = jdbcTemplate.queryForList("""
                    SELECT CAST(p.id_prestamo AS CHAR) AS id_prestamo,
                           CONCAT(TRIM(u.nombre_usuario), ' ', TRIM(u.apellido_usuario)) AS usuario,
                           u.dni_usuario AS dni,
                           u.email_usuario AS correo,
                           l.titulo_libro AS libro,
                           l.isbn AS isbn,
                           CONCAT(DATE_FORMAT(p.fecha_prestamo, '%d/%m/%Y'),
                                  ' ', DATE_FORMAT(p.hora_inicio, '%H:%i')) AS inicio,
                           CONCAT(
                               DATE_FORMAT(
                                   DATE_ADD(p.fecha_prestamo,
                                       INTERVAL IF(p.hora_fin <= p.hora_inicio, 1, 0) DAY),
                                   '%d/%m/%Y'),
                               ' ', DATE_FORMAT(p.hora_fin, '%H:%i')) AS limite,
                           p.estado_prestamo AS estado_codigo
                    FROM prestamo p
                    INNER JOIN usuario u ON p.id_usuario = u.id_usuario
                    INNER JOIN libro l ON p.id_libro = l.id_libro
                    WHERE p.id_reserva = ?
                    ORDER BY p.id_prestamo
                    """, idReserva);

            if (prestamos.isEmpty()) {
                throw new IllegalArgumentException("La reserva seleccionada no tiene préstamos registrados.");
            }

            int numeroDetalle = 1;
            List<String> estados = new ArrayList<>();
            for (Map<String, Object> prestamo : prestamos) {
                String estado = String.valueOf(prestamo.get("estado_codigo"));
                estados.add(estado);
                prestamo.put("numero", String.valueOf(numeroDetalle++));
                prestamo.put("estado", traducirEstadoPrestamo(estado));
            }

            Map<String, Object> primero = prestamos.get(0);
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("logoPath", obtenerLogo());
            parametros.put("numeroComprobante", String.format("PR-%06d", idReserva));
            parametros.put("fechaEmision", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            parametros.put("usuario", String.valueOf(primero.get("usuario")));
            parametros.put("dni", String.valueOf(primero.get("dni")));
            parametros.put("correo", String.valueOf(primero.get("correo")));
            parametros.put("cantidadLibros", String.valueOf(prestamos.size()));
            parametros.put("estadoComprobante", resumirEstadosPrestamo(estados));

            return generarPdf(
                    "/reports/comprobante_prestamo.jrxml",
                    parametros,
                    crearDataSource(prestamos));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el comprobante del préstamo.", ex);
        }
    }

    public byte[] generarReporteEliminacionesLogicas(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            List<Map<String, Object>> bajas = jdbcTemplate.queryForList("""
                    SELECT baja.entidad,
                           baja.id_registro,
                           baja.descripcion_registro,
                           baja.fecha_eliminacion,
                           baja.nombre_responsable,
                           baja.motivo
                    FROM historial_movimientos_logicos baja
                    WHERE MONTH(baja.fecha_eliminacion) = ?
                      AND YEAR(baja.fecha_eliminacion) = ?
                      AND baja.tipo_movimiento = 'BAJA'
                      AND NOT EXISTS (
                          SELECT 1
                          FROM historial_movimientos_logicos baja_posterior
                          WHERE baja_posterior.entidad = baja.entidad
                            AND baja_posterior.id_registro = baja.id_registro
                            AND baja_posterior.tipo_movimiento = 'BAJA'
                            AND baja_posterior.id_auditoria > baja.id_auditoria
                      )
                      AND NOT EXISTS (
                          SELECT 1
                          FROM historial_movimientos_logicos reactivacion
                          WHERE reactivacion.entidad = baja.entidad
                            AND reactivacion.id_registro = baja.id_registro
                            AND reactivacion.tipo_movimiento = 'REACTIVACION'
                            AND reactivacion.id_auditoria > baja.id_auditoria
                      )
                    ORDER BY baja.fecha_eliminacion DESC, baja.entidad, baja.id_registro
                    """, periodo.mes(), periodo.anio());
            long usuarios = 0L;
            long libros = 0L;
            long categorias = 0L;
            long autores = 0L;

            for (Map<String, Object> baja : bajas) {
                String codigoEntidad = String.valueOf(baja.get("entidad"));
                switch (codigoEntidad) {
                    case "USUARIO" -> usuarios++;
                    case "LIBRO" -> libros++;
                    case "CATEGORIA" -> categorias++;
                    case "AUTOR" -> autores++;
                    default -> {
                    }
                }
                baja.put("entidad_nombre", traducirEntidad(codigoEntidad));
                baja.put("id", String.valueOf(baja.get("id_registro")));
                baja.put("fecha", formatearFechaAuditoria(baja.get("fecha_eliminacion")));
                baja.put("responsable", String.valueOf(baja.get("nombre_responsable")));
            }

            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("filtro", "Mensual - " + periodo.nombreMes() + " " + periodo.anio());
            parametros.put("totalRegistros", String.valueOf(bajas.size()));
            parametros.put("totalUsuarios", String.valueOf(usuarios));
            parametros.put("totalLibros", String.valueOf(libros));
            parametros.put("totalCategorias", String.valueOf(categorias));
            parametros.put("totalAutores", String.valueOf(autores));

            return generarPdf(
                    "/reports/reporte_eliminaciones_logicas.jrxml",
                    parametros,
                    crearDataSource(bajas));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de eliminaciones lógicas.", ex);
        }
    }

    public byte[] generarReporteStock(String mes, String anio) {
        Periodo periodo = resolverPeriodo(mes, anio);

        try {
            Map<String, Object> resumenStock = jdbcTemplate.queryForMap("""
                    SELECT COALESCE(SUM(stock), 0) AS total_ejemplares,
                           COUNT(*) AS titulos_registrados,
                           COALESCE(SUM(
                               CASE WHEN stock <= 2 THEN 1 ELSE 0 END
                           ), 0) AS titulos_stock_bajo
                    FROM libro
                    WHERE estado_libro <> 'NO_DISPONIBLE'
                    """);

            List<Map<String, Object>> inventario = jdbcTemplate.queryForList("""
                    SELECT l.titulo_libro AS titulo,
                           c.nombre_categoria AS categoria,
                           l.isbn AS isbn,
                           l.stock AS stock
                    FROM libro l
                    INNER JOIN categoria c ON l.id_categoria = c.id_categoria
                    WHERE l.estado_libro <> 'NO_DISPONIBLE'
                    GROUP BY l.id_libro, l.titulo_libro, c.nombre_categoria,
                             l.isbn, l.stock
                    ORDER BY l.stock ASC, l.titulo_libro ASC
                    """);

            for (Map<String, Object> libro : inventario) {
                libro.put("stock_texto", String.valueOf(numero(libro.get("stock"))));
            }

            Map<String, Object> parametros = parametrosBase(periodo);
            parametros.put("totalEjemplares", String.valueOf(numero(resumenStock.get("total_ejemplares"))));
            parametros.put("titulosRegistrados", String.valueOf(numero(resumenStock.get("titulos_registrados"))));
            parametros.put("titulosStockBajo", String.valueOf(numero(resumenStock.get("titulos_stock_bajo"))));
            return generarPdf(
                    "/reports/reporte_stock.jrxml",
                    parametros,
                    crearDataSource(inventario));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el reporte de stock.", ex);
        }
    }

    private byte[] generarIndicadorResumen(
            String plantilla,
            Periodo periodo,
            String titulo,
            String etiquetaPrincipal,
            String valorPrincipal,
            String detalle1Etiqueta,
            String detalle1Valor,
            String detalle2Etiqueta,
            String detalle2Valor,
            String detalle3Etiqueta,
            String detalle3Valor,
            String conclusion) throws Exception {
        Map<String, Object> parametros = parametrosBase(periodo);
        parametros.put("tituloReporte", titulo);
        parametros.put("etiquetaPrincipal", etiquetaPrincipal);
        parametros.put("valorPrincipal", valorPrincipal);
        parametros.put("detalle1Etiqueta", detalle1Etiqueta);
        parametros.put("detalle1Valor", detalle1Valor);
        parametros.put("detalle2Etiqueta", detalle2Etiqueta);
        parametros.put("detalle2Valor", detalle2Valor);
        parametros.put("detalle3Etiqueta", detalle3Etiqueta);
        parametros.put("detalle3Valor", detalle3Valor);
        parametros.put("mostrarDetalle3", detalle3Etiqueta != null && !detalle3Etiqueta.isBlank());
        parametros.put("conclusion", conclusion);

        return generarPdf(
                plantilla,
                parametros,
                new JREmptyDataSource());
    }

    private String obtenerCategoriaMasLeida(Periodo periodo) {
        try {
            String categoria = jdbcTemplate.queryForObject("""
                    SELECT c.nombre_categoria
                    FROM prestamo p
                    INNER JOIN libro l ON p.id_libro = l.id_libro
                    INNER JOIN categoria c ON l.id_categoria = c.id_categoria
                    WHERE MONTH(p.fecha_prestamo) = ?
                      AND YEAR(p.fecha_prestamo) = ?
                      AND p.estado_prestamo <> 'CANCELADO'
                    GROUP BY c.id_categoria, c.nombre_categoria
                    ORDER BY COUNT(*) DESC, c.nombre_categoria ASC
                    LIMIT 1
                    """, String.class, periodo.mes(), periodo.anio());
            return categoria == null || categoria.isBlank() ? "Ninguna" : categoria;
        } catch (EmptyResultDataAccessException ex) {
            return "Ninguna";
        }
    }

    private Map<String, Object> parametrosBase(Periodo periodo) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("periodo", "Mensual - " + periodo.nombreMes() + " " + periodo.anio());
        parametros.put("fechaEmision", LocalDate.now().format(FORMATO_FECHA));
        parametros.put("logoPath", obtenerLogo());
        return parametros;
    }

    private Map<String, Object> parametrosRango(LocalDate fechaInicio, LocalDate fechaFin) {
        DateTimeFormatter formatoRango = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("periodo", "Del " + fechaInicio.format(formatoRango)
                + " al " + fechaFin.format(formatoRango));
        parametros.put("fechaEmision", LocalDate.now().format(FORMATO_FECHA));
        parametros.put("logoPath", obtenerLogo());
        return parametros;
    }

    private URL obtenerLogo() {
        URL logo = getClass().getResource("/static/img/Logo_LuzdelSaber.png");
        if (logo == null) {
            throw new IllegalStateException("No se encontró el logotipo para el reporte.");
        }
        return logo;
    }

    private void validarRangoFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Debes indicar la fecha inicial y la fecha final.");
        }
        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final.");
        }
    }

    private String traducirEstadoPrestamo(String estado) {
        return switch (estado) {
            case "ACTIVO" -> "Activo";
            case "DEVUELTO" -> "Devuelto";
            case "DEVUELTO_TARDIO" -> "Devuelto con retraso";
            case "CANCELADO" -> "Cancelado";
            default -> estado == null || estado.isBlank() ? "Sin estado" : estado;
        };
    }

    private String resumirEstadosPrestamo(List<String> estados) {
        if (estados.stream().anyMatch("ACTIVO"::equals)) {
            return "Préstamo activo";
        }
        if (estados.stream().anyMatch("DEVUELTO_TARDIO"::equals)) {
            return "Devuelto con retraso";
        }
        if (estados.stream().allMatch("DEVUELTO"::equals)) {
            return "Devuelto";
        }
        if (estados.stream().allMatch("CANCELADO"::equals)) {
            return "Cancelado";
        }
        return "Estado mixto";
    }

    private String traducirEntidad(String entidad) {
        return switch (entidad) {
            case "USUARIO" -> "Usuario";
            case "LIBRO" -> "Libro";
            case "CATEGORIA" -> "Categoría";
            case "AUTOR" -> "Autor";
            default -> entidad;
        };
    }

    private String formatearFechaAuditoria(Object fecha) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        if (fecha instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(formato);
        }
        if (fecha instanceof LocalDateTime fechaHora) {
            return fechaHora.format(formato);
        }
        return "Sin fecha registrada";
    }


    private byte[] generarPdf(String plantilla, Map<String, Object> parametros,
            net.sf.jasperreports.engine.JRDataSource dataSource) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream(plantilla)) {
            if (stream == null) {
                throw new IllegalStateException("No se encontró la plantilla " + plantilla + ".");
            }
            JasperReport reporte = JasperCompileManager.compileReport(stream);
            JasperPrint impresion = JasperFillManager.fillReport(reporte, parametros, dataSource);
            return JasperExportManager.exportReportToPdf(impresion);
        }
    }

    private JRMapCollectionDataSource crearDataSource(List<Map<String, Object>> filas) {
        List<Map<String, ?>> filasReporte = new ArrayList<>(filas.size());
        for (Map<String, Object> fila : filas) {
            filasReporte.add(fila);
        }
        return new JRMapCollectionDataSource(filasReporte);
    }

    private Periodo resolverPeriodo(String mes, String anio) {
        LocalDate hoy = LocalDate.now();
        int numeroMes = mes == null || mes.isBlank() ? hoy.getMonthValue() : obtenerNumeroMes(mes);
        int numeroAnio;

        try {
            numeroAnio = anio == null || anio.isBlank() ? hoy.getYear() : Integer.parseInt(anio.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("El año del reporte no es válido.", ex);
        }

        if (numeroAnio < 2000 || numeroAnio > 2100) {
            throw new IllegalArgumentException("El año del reporte no es válido.");
        }

        return new Periodo(numeroMes, numeroAnio, nombreMes(numeroMes));
    }

    private int obtenerNumeroMes(String mes) {
        return switch (mes.toLowerCase(Locale.ROOT).trim()) {
            case "enero" -> 1;
            case "febrero" -> 2;
            case "marzo" -> 3;
            case "abril" -> 4;
            case "mayo" -> 5;
            case "junio" -> 6;
            case "julio" -> 7;
            case "agosto" -> 8;
            case "septiembre" -> 9;
            case "octubre" -> 10;
            case "noviembre" -> 11;
            case "diciembre" -> 12;
            default -> throw new IllegalArgumentException("El mes del reporte no es válido.");
        };
    }

    private String nombreMes(int mes) {
        return switch (mes) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> throw new IllegalArgumentException("El mes del reporte no es válido.");
        };
    }

    private int valorOZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private long valorOZero(Long valor) {
        return valor == null ? 0L : valor;
    }

    private double valorOZero(Double valor) {
        return valor == null ? 0.0 : valor;
    }

    private double redondearUnaCifra(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private long numero(Object valor) {
        return valor instanceof Number numero ? numero.longValue() : 0L;
    }

    private record Periodo(int mes, int anio, String nombreMes) {
    }
}
