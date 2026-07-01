package com.luzdelsaber.biblioteca.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DashboardData obtenerDashboard() {
        long reservasHoy = contar("""
                select count(*)
                from reserva
                where fecha_reserva = current_date()
                  and estado_reserva = 'ACTIVA'
                """);
        long prestamosVencidos = contar("""
                select count(*)
                from (
                    select p.id_prestamo as id_vencido
                    from prestamo p
                    where p.estado_prestamo = 'ACTIVO'
                      and timestamp(p.fecha_prestamo, p.hora_fin) < current_timestamp()
                    union all
                    select distinct r.id_reserva as id_vencido
                    from reserva r
                    inner join reserva_detalle rd on rd.id_reserva = r.id_reserva
                    where r.estado_reserva = 'VENCIDA'
                       or (
                            r.estado_reserva = 'ACTIVA'
                            and timestamp(rd.fecha_recojo_limite, rd.hora_recojo_limite) < current_timestamp()
                       )
                ) vencidos
                """);
        long incidenciasPendientes = contar("""
                select count(*)
                from incidencia i
                inner join prestamo p on p.id_prestamo = i.id_prestamo
                where not exists (
                    select 1
                    from sancion s
                    where s.id_incidencia = i.id_incidencia
                )
                  and (
                        i.tipo_incidencia <> 'RETRASO'
                        or (
                            select count(distinct coalesce(p2.id_reserva, p2.id_prestamo))
                            from incidencia i2
                            inner join prestamo p2 on p2.id_prestamo = i2.id_prestamo
                            where p2.id_usuario = p.id_usuario
                              and i2.tipo_incidencia = 'RETRASO'
                              and p2.fecha_prestamo >= date_format(current_date(), '%Y-%m-01')
                              and p2.fecha_prestamo < date_add(date_format(current_date(), '%Y-%m-01'), interval 1 month)
                        ) >= 3
                  )
                """);
        long prestamosDevueltosTardios = contar("""
                select count(distinct coalesce(id_reserva, id_prestamo))
                from prestamo
                where (
                        estado_prestamo = 'DEVUELTO_TARDIO'
                        or (
                        estado_prestamo = 'DEVUELTO'
                        and fecha_devolucion_real is not null
                        and hora_devolucion_real is not null
                        and timestamp(fecha_devolucion_real, hora_devolucion_real) > timestamp(fecha_prestamo, hora_fin)
                        )
                  )
                  and fecha_devolucion_real >= date_format(current_date(), '%Y-%m-01')
                  and fecha_devolucion_real < date_add(date_format(current_date(), '%Y-%m-01'), interval 1 month)
                """);
        long stockBajo = contar("""
                select count(*)
                from libro
                where stock <= 2
                """);

        List<DashboardMetric> metricas = List.of(
                new DashboardMetric("Reservas hoy", reservasHoy, "Programadas para atender", "fa-calendar-day", "metric-blue"),
                new DashboardMetric("Reservas activas", contar("""
                        select count(*)
                        from reserva
                        where estado_reserva = 'ACTIVA'
                        """), "Solicitudes pendientes", "fa-bookmark", "metric-green"),
                new DashboardMetric("Prestamos activos", contar("""
                        select count(*)
                        from prestamo
                        where estado_prestamo = 'ACTIVO'
                        """), "Libros entregados", "fa-hand-holding", "metric-yellow"),
                new DashboardMetric("Prestamos vencidos", prestamosVencidos, "Requieren seguimiento", "fa-clock", "metric-red"),
                new DashboardMetric("Devueltos tardios", prestamosDevueltosTardios, "Retrasos del mes", "fa-calendar-xmark", "metric-red"),
                new DashboardMetric("Incidencias pendientes", incidenciasPendientes, "Sanciones disponibles", "fa-triangle-exclamation", "metric-orange"),
                new DashboardMetric("Stock bajo", stockBajo, "Libros con 2 o menos", "fa-boxes-stacked", "metric-purple"));

        return new DashboardData(
                metricas,
                contarPorEstado("reserva", "estado_reserva", "reserva"),
                contarPorEstado("prestamo", "estado_prestamo", "prestamo"),
                listarReservasRecientes(),
                listarLibrosSolicitados(),
                listarProximasReservas(),
                listarPrestamosVencidos(),
                listarStockBajo());
    }

    private long contar(String sql) {
        Long total = jdbcTemplate.queryForObject(sql, Long.class);
        return total == null ? 0 : total;
    }

    private List<EstadoConteo> contarPorEstado(String tabla, String columna, String tipo) {
        return jdbcTemplate.query("""
                select %s as estado, count(*) as total
                from %s
                group by %s
                order by total desc, estado asc
                """.formatted(columna, tabla, columna),
                (rs, rowNum) -> new EstadoConteo(
                        rs.getString("estado"),
                        rs.getLong("total"),
                        estiloEstado(tipo, rs.getString("estado"))));
    }

    private List<ActividadReserva> listarReservasRecientes() {
        return jdbcTemplate.query("""
                select r.id_reserva,
                       concat(u.nombre_usuario, ' ', u.apellido_usuario) as usuario,
                       r.fecha_reserva,
                       min(rd.hora_reserva) as hora_reserva,
                       count(rd.id_detalle) as libros,
                       case
                           when r.estado_reserva = 'DEVUELTA'
                                and exists (
                                    select 1
                                    from prestamo p
                                    where p.id_reserva = r.id_reserva
                                      and p.estado_prestamo = 'DEVUELTO_TARDIO'
                                )
                               then 'DEVUELTO TARDIO'
                           else r.estado_reserva
                       end as estado_reserva
                from reserva r
                inner join usuario u on u.id_usuario = r.id_usuario
                inner join reserva_detalle rd on rd.id_reserva = r.id_reserva
                group by r.id_reserva, u.nombre_usuario, u.apellido_usuario, r.fecha_reserva, r.estado_reserva
                order by r.fecha_reserva desc, hora_reserva desc, r.id_reserva desc
                limit 5
                """, (rs, rowNum) -> new ActividadReserva(
                rs.getInt("id_reserva"),
                rs.getString("usuario"),
                leerFecha(rs, "fecha_reserva"),
                leerHora(rs, "hora_reserva"),
                rs.getInt("libros"),
                rs.getString("estado_reserva"),
                estiloEstado("reserva", rs.getString("estado_reserva"))));
    }

    private List<LibroSolicitado> listarLibrosSolicitados() {
        return jdbcTemplate.query("""
                select l.titulo_libro,
                       l.isbn,
                       l.estado_libro,
                       count(rd.id_detalle) as solicitudes
                from reserva_detalle rd
                inner join libro l on l.id_libro = rd.id_libro
                group by l.id_libro, l.titulo_libro, l.isbn, l.estado_libro
                order by solicitudes desc, l.titulo_libro asc
                limit 5
                """, (rs, rowNum) -> new LibroSolicitado(
                rs.getString("titulo_libro"),
                rs.getString("isbn"),
                rs.getString("estado_libro"),
                rs.getLong("solicitudes"),
                estiloLibro(rs.getString("estado_libro"))));
    }

    private List<ActividadReserva> listarProximasReservas() {
        return jdbcTemplate.query("""
                select r.id_reserva,
                       concat(u.nombre_usuario, ' ', u.apellido_usuario) as usuario,
                       r.fecha_reserva,
                       min(rd.hora_reserva) as hora_reserva,
                       count(rd.id_detalle) as libros,
                       r.estado_reserva
                from reserva r
                inner join usuario u on u.id_usuario = r.id_usuario
                inner join reserva_detalle rd on rd.id_reserva = r.id_reserva
                where r.estado_reserva = 'ACTIVA'
                  and r.fecha_reserva >= current_date()
                  and timestamp(rd.fecha_recojo_limite, rd.hora_recojo_limite) >= current_timestamp()
                group by r.id_reserva, u.nombre_usuario, u.apellido_usuario, r.fecha_reserva, r.estado_reserva
                order by r.fecha_reserva asc, hora_reserva asc, r.id_reserva asc
                limit 5
                """, (rs, rowNum) -> new ActividadReserva(
                rs.getInt("id_reserva"),
                rs.getString("usuario"),
                leerFecha(rs, "fecha_reserva"),
                leerHora(rs, "hora_reserva"),
                rs.getInt("libros"),
                rs.getString("estado_reserva"),
                estiloEstado("reserva", rs.getString("estado_reserva"))));
    }

    private List<PrestamoVencido> listarPrestamosVencidos() {
        return jdbcTemplate.query("""
                select *
                from (
                    select concat(u.nombre_usuario, ' ', u.apellido_usuario) as usuario,
                           l.titulo_libro as titulo_libro,
                           p.fecha_prestamo as fecha_prestamo,
                           p.hora_fin as hora_fin
                    from prestamo p
                    inner join usuario u on u.id_usuario = p.id_usuario
                    inner join libro l on l.id_libro = p.id_libro
                    where p.estado_prestamo = 'ACTIVO'
                      and timestamp(p.fecha_prestamo, p.hora_fin) < current_timestamp()
                    union all
                    select concat(u.nombre_usuario, ' ', u.apellido_usuario) as usuario,
                           concat(count(rd.id_detalle), ' libro(s) reservados') as titulo_libro,
                           r.fecha_reserva as fecha_prestamo,
                           max(rd.hora_recojo_limite) as hora_fin
                    from reserva r
                    inner join usuario u on u.id_usuario = r.id_usuario
                    inner join reserva_detalle rd on rd.id_reserva = r.id_reserva
                    where r.estado_reserva = 'VENCIDA'
                       or (
                            r.estado_reserva = 'ACTIVA'
                            and timestamp(rd.fecha_recojo_limite, rd.hora_recojo_limite) < current_timestamp()
                       )
                    group by r.id_reserva, u.nombre_usuario, u.apellido_usuario, r.fecha_reserva
                ) vencidos
                order by fecha_prestamo asc, hora_fin asc
                limit 5
                """, (rs, rowNum) -> new PrestamoVencido(
                rs.getString("usuario"),
                rs.getString("titulo_libro"),
                leerFecha(rs, "fecha_prestamo"),
                leerHora(rs, "hora_fin")));
    }

    private List<StockLibro> listarStockBajo() {
        return jdbcTemplate.query("""
                select titulo_libro, isbn, stock, estado_libro
                from libro
                where stock <= 2
                order by stock asc, titulo_libro asc
                limit 5
                """, (rs, rowNum) -> new StockLibro(
                rs.getString("titulo_libro"),
                rs.getString("isbn"),
                rs.getInt("stock"),
                rs.getString("estado_libro"),
                estiloLibro(rs.getString("estado_libro"))));
    }

    private LocalDate leerFecha(ResultSet rs, String columna) throws SQLException {
        java.sql.Date fecha = rs.getDate(columna);
        return fecha == null ? null : fecha.toLocalDate();
    }

    private LocalTime leerHora(ResultSet rs, String columna) throws SQLException {
        java.sql.Time hora = rs.getTime(columna);
        return hora == null ? null : hora.toLocalTime();
    }

    private String estiloEstado(String tipo, String estado) {
        if (estado == null) {
            return "text-bg-secondary";
        }
        return switch (estado) {
            case "ACTIVA", "ACTIVO" -> "text-bg-success";
            case "PRESTADA" -> "text-bg-primary";
            case "DEVUELTA", "DEVUELTO" -> "text-bg-secondary";
            case "DEVUELTO_TARDIO", "DEVUELTO TARDIO" -> "text-bg-danger";
            case "CANCELADA", "CANCELADO" -> "text-bg-danger";
            default -> "prestamo".equals(tipo) ? "text-bg-dark" : "text-bg-warning";
        };
    }

    private String estiloLibro(String estado) {
        if (estado == null) {
            return "text-bg-secondary";
        }
        return switch (estado) {
            case "DISPONIBLE" -> "text-bg-success";
            case "RESERVADO" -> "text-bg-warning";
            case "PRESTADO" -> "text-bg-primary";
            case "NO_DISPONIBLE" -> "text-bg-secondary";
            default -> "text-bg-dark";
        };
    }

    public record DashboardData(
            List<DashboardMetric> metricas,
            List<EstadoConteo> reservasPorEstado,
            List<EstadoConteo> prestamosPorEstado,
            List<ActividadReserva> reservasRecientes,
            List<LibroSolicitado> librosSolicitados,
            List<ActividadReserva> proximasReservas,
            List<PrestamoVencido> prestamosVencidos,
            List<StockLibro> stockBajo) {
    }

    public record DashboardMetric(String titulo, long total, String detalle, String icono, String estilo) {
    }

    public record EstadoConteo(String estado, long total, String estilo) {
    }

    public record ActividadReserva(
            Integer idReserva,
            String usuario,
            LocalDate fechaReserva,
            LocalTime horaReserva,
            Integer libros,
            String estado,
            String estilo) {
    }

    public record LibroSolicitado(String titulo, String isbn, String estado, long solicitudes, String estilo) {
    }

    public record PrestamoVencido(String usuario, String libro, LocalDate fechaPrestamo, LocalTime horaFin) {
    }

    public record StockLibro(String titulo, String isbn, Integer stock, String estado, String estilo) {
    }
}
