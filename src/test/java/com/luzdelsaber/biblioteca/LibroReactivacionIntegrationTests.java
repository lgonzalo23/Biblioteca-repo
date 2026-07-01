package com.luzdelsaber.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.luzdelsaber.biblioteca.exception.BibliografiaValidationException;
import com.luzdelsaber.biblioteca.service.BibliografiaService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class LibroReactivacionIntegrationTests {

    @Autowired
    private BibliografiaService bibliografiaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void muestraReactivarSoloParaLaBajaLogicaYRegistraElMovimiento() throws Exception {
        Integer idLibro = obtenerLibroConStock();
        jdbcTemplate.update(
                "UPDATE libro SET estado_libro = 'NO_DISPONIBLE' WHERE id_libro = ?",
                idLibro);

        mockMvc.perform(get("/libros")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ENCARGADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/libros/" + idLibro + "/reactivar")));

        mockMvc.perform(post("/libros/{idLibro}/reactivar", idLibro)
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ENCARGADO"))
                .andExpect(status().is3xxRedirection());

        assertEquals("DISPONIBLE", jdbcTemplate.queryForObject(
                "SELECT estado_libro FROM libro WHERE id_libro = ?",
                String.class,
                idLibro));
        assertEquals("REACTIVACION", jdbcTemplate.queryForObject("""
                SELECT tipo_movimiento
                FROM auditoria_eliminacion_logica
                WHERE entidad = 'LIBRO' AND id_registro = ?
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, String.class, idLibro));
    }

    @Test
    void impideReactivarUnLibroSinStock() {
        Integer idLibro = obtenerLibroConStock();
        jdbcTemplate.update("""
                UPDATE libro
                SET estado_libro = 'NO_DISPONIBLE', stock = 0
                WHERE id_libro = ?
                """, idLibro);

        assertThrows(
                BibliografiaValidationException.class,
                () -> bibliografiaService.reactivarLibro(idLibro, 0, "Encargado de prueba"));
    }

    private Integer obtenerLibroConStock() {
        return jdbcTemplate.queryForObject("""
                SELECT id_libro
                FROM libro
                WHERE stock > 0
                ORDER BY id_libro
                LIMIT 1
                """, Integer.class);
    }
}
