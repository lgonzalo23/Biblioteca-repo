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
        agregarColumnaSiNoExiste("libro", "url_imagen", "VARCHAR(255) NULL");
        agregarColumnaSiNoExiste("categoria", "estado_categoria", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'");
        agregarColumnaSiNoExiste("autor", "estado_autor", "VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'");

        jdbcTemplate.update("""
                update libro
                set estado_libro = 'NO DISPONIBLE'
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
}
