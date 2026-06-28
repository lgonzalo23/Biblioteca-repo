package com.luzdelsaber.biblioteca.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BibliografiaSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public BibliografiaSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        crearTablaPrestamoSiNoExiste();
        crearTablaIncidenciaSiNoExiste();
        crearTablaSancionSiNoExiste();
        adaptarTablaSancion();
        migrarTiposDeIncidenciaYSancion();
        agregarColumnaSiNoExiste("prestamo", "fecha_devolucion_real", "DATE NULL");
        agregarColumnaSiNoExiste("prestamo", "hora_devolucion_real", "TIME NULL");
        migrarDevolucionesRegistradas();
        marcarPrestamosDevueltosTardios();
        agregarColumnaSiNoExiste("libro", "url_imagen", "VARCHAR(255) NULL");
        agregarColumnaSiNoExiste("categoria", "estado_categoria", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'");
        agregarColumnaSiNoExiste("autor", "estado_autor", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'");
        agregarColumnaSiNoExiste("reserva_detalle", "hora_reserva", "TIME NOT NULL DEFAULT '08:00:00'");
        agregarColumnaSiNoExiste("reserva_detalle", "horas_prestamo", "INT NOT NULL DEFAULT 1");
        agregarColumnaSiNoExiste("reserva_detalle", "hora_recojo_limite", "TIME NOT NULL DEFAULT '09:00:00'");
        eliminarColumnaSiExiste("reserva", "hora_reserva");
        eliminarColumnaSiExiste("reserva", "horas_prestamo");
        eliminarColumnaSiExiste("reserva", "id_libro");
        eliminarColumnaSiExiste("reserva", "fecha_registro");
        eliminarColumnaSiExiste("reserva", "posicion_cola");

        jdbcTemplate.update("""
                update libro
                set estado_libro = 'NO_DISPONIBLE'
                where estado_libro = 'INACTIVO'
                """);
    }

    private void agregarColumnaSiNoExiste(String tabla, String columna, String definicion) {
        Integer existe = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tabla, columna);

        if (existe == null || existe == 0) {
            jdbcTemplate.execute("alter table " + tabla + " add column " + columna + " " + definicion);
        }
    }

    private void crearTablaPrestamoSiNoExiste() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS prestamo (
                    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
                    id_reserva INT NULL,
                    id_usuario INT NOT NULL,
                    id_libro INT NOT NULL,
                    fecha_prestamo DATE NOT NULL,
                    hora_inicio TIME NOT NULL,
                    hora_fin TIME NOT NULL,
                    fecha_devolucion_real DATE NULL,
                    hora_devolucion_real TIME NULL,
                    estado_prestamo VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
                    CONSTRAINT fk_prestamo_reserva
                        FOREIGN KEY (id_reserva) REFERENCES reserva(id_reserva),
                    CONSTRAINT fk_prestamo_usuario
                        FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario),
                    CONSTRAINT fk_prestamo_libro
                        FOREIGN KEY (id_libro) REFERENCES libro(id_libro)
                )
                """);
        agregarColumnaSiNoExiste("prestamo", "id_reserva", "INT NULL");
    }

    private void crearTablaIncidenciaSiNoExiste() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS incidencia (
                    id_incidencia INT AUTO_INCREMENT PRIMARY KEY,
                    id_prestamo INT NOT NULL,
                    tipo_incidencia VARCHAR(50) NOT NULL,
                    descripcion_incidencia VARCHAR(200) NOT NULL,
                    fecha_incidencia DATE NOT NULL,
                    CONSTRAINT fk_incidencia_prestamo
                        FOREIGN KEY (id_prestamo) REFERENCES prestamo(id_prestamo)
                )
                """);
    }

    private void crearTablaSancionSiNoExiste() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sancion (
                    id_sancion INT AUTO_INCREMENT PRIMARY KEY,
                    id_incidencia INT NOT NULL,
                    tipo_sancion VARCHAR(50) NOT NULL,
                    dias_suspension INT NULL,
                    estado_sancion VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
                    CONSTRAINT fk_sancion_incidencia
                        FOREIGN KEY (id_incidencia) REFERENCES incidencia(id_incidencia)
                )
                """);
    }

    private void adaptarTablaSancion() {
        if (existeTabla("sancion")) {
            jdbcTemplate.execute("alter table sancion modify column dias_suspension INT NULL");
        }
    }

    private void migrarTiposDeIncidenciaYSancion() {
        if (existeTabla("incidencia")) {
            jdbcTemplate.update("""
                    update incidencia
                    set tipo_incidencia = 'DAÑO'
                    where tipo_incidencia = 'DANO'
                    """);
        }
        if (existeTabla("sancion")) {
            jdbcTemplate.update("""
                    update sancion
                    set tipo_sancion = 'SUSPENSION_TEMPORAL'
                    where tipo_sancion in ('SUSPENSION', 'BLOQUEO_TEMPORAL')
                    """);
            jdbcTemplate.update("""
                    update sancion
                    set tipo_sancion = 'SUSPENSION_TOTAL',
                        dias_suspension = null
                    where tipo_sancion = 'REVISION_ADMINISTRATIVA'
                    """);
        }
    }

    private void migrarDevolucionesRegistradas() {
        if (!existeTabla("prestamo")) {
            return;
        }
        jdbcTemplate.update("""
                update prestamo
                set fecha_devolucion_real = fecha_prestamo
                where estado_prestamo = 'DEVUELTO'
                  and fecha_devolucion_real is null
                """);
        jdbcTemplate.update("""
                update prestamo
                set hora_devolucion_real = hora_fin
                where estado_prestamo = 'DEVUELTO'
                  and hora_devolucion_real is null
                """);
    }

    private void marcarPrestamosDevueltosTardios() {
        if (!existeTabla("prestamo")) {
            return;
        }
        jdbcTemplate.update("""
                update prestamo
                set estado_prestamo = 'DEVUELTO_TARDIO'
                where estado_prestamo = 'DEVUELTO'
                  and fecha_devolucion_real is not null
                  and hora_devolucion_real is not null
                  and timestamp(fecha_devolucion_real, hora_devolucion_real) > timestamp(fecha_prestamo, hora_fin)
                """);
    }

    private boolean existeTabla(String tabla) {
        Integer existe = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = ?
                """, Integer.class, tabla);
        return existe != null && existe > 0;
    }

    private void eliminarColumnaSiExiste(String tabla, String columna) {
        Integer existe = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                """, Integer.class, tabla, columna);

        if (existe != null && existe > 0) {
            eliminarLlavesForaneasDeColumna(tabla, columna);
            jdbcTemplate.execute("alter table " + tabla + " drop column " + columna);
        }
    }

    private void eliminarLlavesForaneasDeColumna(String tabla, String columna) {
        jdbcTemplate.queryForList("""
                select constraint_name
                from information_schema.key_column_usage
                where table_schema = database()
                  and table_name = ?
                  and column_name = ?
                  and referenced_table_name is not null
                """, String.class, tabla, columna)
                .forEach(nombreLlave -> jdbcTemplate.execute("alter table " + tabla + " drop foreign key " + nombreLlave));
    }
}
