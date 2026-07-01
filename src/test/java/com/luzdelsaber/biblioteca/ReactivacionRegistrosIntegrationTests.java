package com.luzdelsaber.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.luzdelsaber.biblioteca.model.Reserva;
import com.luzdelsaber.biblioteca.service.BibliografiaService;
import com.luzdelsaber.biblioteca.service.ReservaService;
import com.luzdelsaber.biblioteca.service.UsuarioService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class ReactivacionRegistrosIntegrationTests {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private BibliografiaService bibliografiaService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void reactivaUsuarioInactivoSinSancionYRegistraAuditoria() throws Exception {
        Integer idUsuario = jdbcTemplate.queryForObject("""
                SELECT u.id_usuario
                FROM usuario u
                WHERE u.estado_usuario = 'ACTIVO'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM sancion s
                      INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
                      INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
                      WHERE p.id_usuario = u.id_usuario
                        AND s.estado_sancion = 'ACTIVA'
                  )
                ORDER BY u.id_usuario
                LIMIT 1
                """, Integer.class);

        usuarioService.eliminarLogico(idUsuario, 0, "Encargado de prueba");
        entityManager.clear();

        mockMvc.perform(get("/usuarios")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "/usuarios/" + idUsuario + "/reactivar")));

        usuarioService.reactivar(idUsuario, 0, "Encargado de prueba");

        assertEquals("ACTIVO", jdbcTemplate.queryForObject(
                "SELECT estado_usuario FROM usuario WHERE id_usuario = ?",
                String.class,
                idUsuario));
        assertEquals("REACTIVACION", ultimoMovimiento("USUARIO", idUsuario));
    }

    @Test
    void reactivaAutomaticamenteUnaSuspensionTemporalVencida() {
        Map<String, Object> datos = jdbcTemplate.queryForMap("""
                SELECT u.id_usuario, l.id_libro
                FROM usuario u
                CROSS JOIN libro l
                WHERE u.estado_usuario = 'ACTIVO'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM sancion s
                      INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
                      INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
                      WHERE p.id_usuario = u.id_usuario
                        AND s.estado_sancion = 'ACTIVA'
                  )
                ORDER BY u.id_usuario, l.id_libro
                LIMIT 1
                """);
        Integer idUsuario = ((Number) datos.get("id_usuario")).intValue();
        Integer idLibro = ((Number) datos.get("id_libro")).intValue();

        jdbcTemplate.update("""
                INSERT INTO prestamo
                    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin,
                     estado_prestamo, incidencia_revisada)
                VALUES (?, ?, ?, ?, ?, 'DEVUELTO', true)
                """,
                idUsuario,
                idLibro,
                LocalDate.now().minusDays(3),
                LocalTime.of(9, 0),
                LocalTime.of(10, 0));
        Integer idPrestamo = jdbcTemplate.queryForObject(
                "SELECT MAX(id_prestamo) FROM prestamo WHERE id_usuario = ?",
                Integer.class,
                idUsuario);

        jdbcTemplate.update("""
                INSERT INTO incidencia
                    (id_prestamo, tipo_incidencia, descripcion_incidencia, fecha_incidencia, fecha_registro)
                VALUES (?, 'DAÑO', 'Incidencia temporal de prueba', ?, ?)
                """,
                idPrestamo,
                LocalDate.now().minusDays(3),
                LocalDate.now().minusDays(2));
        Integer idIncidencia = jdbcTemplate.queryForObject(
                "SELECT MAX(id_incidencia) FROM incidencia WHERE id_prestamo = ?",
                Integer.class,
                idPrestamo);

        jdbcTemplate.update("""
                INSERT INTO sancion
                    (id_incidencia, tipo_sancion, dias_suspension, estado_sancion)
                VALUES (?, 'SUSPENSION_TEMPORAL', 1, 'ACTIVA')
                """,
                idIncidencia);
        Integer idSancion = jdbcTemplate.queryForObject(
                "SELECT MAX(id_sancion) FROM sancion WHERE id_incidencia = ?",
                Integer.class,
                idIncidencia);
        jdbcTemplate.update(
                "UPDATE usuario SET estado_usuario = 'INACTIVO' WHERE id_usuario = ?",
                idUsuario);
        entityManager.clear();

        assertTrue(usuarioService.obtenerMensajeBloqueoLogin(
                usuarioService.buscarPorId(idUsuario).orElseThrow()).isEmpty());
        assertEquals("ACTIVO", jdbcTemplate.queryForObject(
                "SELECT estado_usuario FROM usuario WHERE id_usuario = ?",
                String.class,
                idUsuario));
        assertEquals("INACTIVA", jdbcTemplate.queryForObject(
                "SELECT estado_sancion FROM sancion WHERE id_sancion = ?",
                String.class,
                idSancion));
    }

    @Test
    void registraLaIncidenciaConLaFechaDelPrestamo() {
        Map<String, Object> datos = jdbcTemplate.queryForMap("""
                SELECT u.id_usuario, l.id_libro
                FROM usuario u
                CROSS JOIN libro l
                WHERE u.estado_usuario = 'ACTIVO'
                ORDER BY u.id_usuario, l.id_libro
                LIMIT 1
                """);
        Integer idUsuario = ((Number) datos.get("id_usuario")).intValue();
        Integer idLibro = ((Number) datos.get("id_libro")).intValue();
        LocalDate fechaPrestamo = LocalDate.now().minusDays(8);

        jdbcTemplate.update("""
                INSERT INTO prestamo
                    (id_usuario, id_libro, fecha_prestamo, hora_inicio, hora_fin,
                     fecha_devolucion_real, hora_devolucion_real, estado_prestamo, incidencia_revisada)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'DEVUELTO', false)
                """,
                idUsuario,
                idLibro,
                fechaPrestamo,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                fechaPrestamo,
                LocalTime.of(9, 45));
        Integer idPrestamo = jdbcTemplate.queryForObject("""
                SELECT id_prestamo
                FROM prestamo
                WHERE id_usuario = ?
                  AND id_libro = ?
                  AND fecha_prestamo = ?
                  AND hora_inicio = '09:00:00'
                ORDER BY id_prestamo DESC
                LIMIT 1
                """, Integer.class, idUsuario, idLibro, fechaPrestamo);

        reservaService.registrarIncidencia(idPrestamo, "PERDIDA", "Incidencia de prueba");

        assertEquals(fechaPrestamo, jdbcTemplate.queryForObject(
                "SELECT fecha_incidencia FROM incidencia WHERE id_prestamo = ?",
                LocalDate.class,
                idPrestamo));
        assertEquals(LocalDate.now(), jdbcTemplate.queryForObject(
                "SELECT fecha_registro FROM incidencia WHERE id_prestamo = ?",
                LocalDate.class,
                idPrestamo));
    }

    @Test
    void reactivaCategoriaYAutorYRegistraAmbosMovimientos() throws Exception {
        Integer idCategoria = jdbcTemplate.queryForObject("""
                SELECT id_categoria FROM categoria
                WHERE estado_categoria = 'ACTIVO'
                ORDER BY id_categoria LIMIT 1
                """, Integer.class);
        Integer idAutor = jdbcTemplate.queryForObject("""
                SELECT id_autor FROM autor
                WHERE estado_autor = 'ACTIVO'
                ORDER BY id_autor LIMIT 1
                """, Integer.class);

        bibliografiaService.eliminarCategoriaLogico(idCategoria, 0, "Encargado de prueba");
        bibliografiaService.eliminarAutorLogico(idAutor, 0, "Encargado de prueba");

        mockMvc.perform(get("/libros")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ENCARGADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "/libros/categorias/" + idCategoria + "/reactivar")))
                .andExpect(content().string(Matchers.containsString(
                        "/libros/autores/" + idAutor + "/reactivar")));

        bibliografiaService.reactivarCategoria(idCategoria, 0, "Encargado de prueba");
        bibliografiaService.reactivarAutor(idAutor, 0, "Encargado de prueba");
        entityManager.flush();

        assertEquals("ACTIVO", jdbcTemplate.queryForObject(
                "SELECT estado_categoria FROM categoria WHERE id_categoria = ?",
                String.class,
                idCategoria));
        assertEquals("ACTIVO", jdbcTemplate.queryForObject(
                "SELECT estado_autor FROM autor WHERE id_autor = ?",
                String.class,
                idAutor));
        assertEquals("REACTIVACION", ultimoMovimiento("CATEGORIA", idCategoria));
        assertEquals("REACTIVACION", ultimoMovimiento("AUTOR", idAutor));
    }

    @Test
    void reactivaPedidoCanceladoYVuelveADescontarStock() throws Exception {
        Map<String, Object> datos = jdbcTemplate.queryForMap("""
                SELECT u.id_usuario, l.id_libro, l.stock
                FROM usuario u
                CROSS JOIN libro l
                WHERE u.estado_usuario = 'ACTIVO'
                  AND l.estado_libro = 'DISPONIBLE'
                  AND l.stock > 0
                  AND NOT EXISTS (
                      SELECT 1
                      FROM sancion s
                      INNER JOIN incidencia i ON i.id_incidencia = s.id_incidencia
                      INNER JOIN prestamo p ON p.id_prestamo = i.id_prestamo
                      WHERE p.id_usuario = u.id_usuario
                        AND s.estado_sancion = 'ACTIVA'
                  )
                  AND NOT EXISTS (
                      SELECT 1
                      FROM reserva r
                      INNER JOIN reserva_detalle rd ON rd.id_reserva = r.id_reserva
                      WHERE r.id_usuario = u.id_usuario
                        AND rd.id_libro = l.id_libro
                        AND r.estado_reserva = 'ACTIVA'
                        AND rd.estado_detalle_reserva = 'ACTIVA'
                  )
                ORDER BY u.id_usuario, l.id_libro
                LIMIT 1
                """);
        Integer idUsuario = ((Number) datos.get("id_usuario")).intValue();
        Integer idLibro = ((Number) datos.get("id_libro")).intValue();
        int stockInicial = ((Number) datos.get("stock")).intValue();

        Reserva reserva = reservaService.crearReserva(
                idUsuario,
                List.of(idLibro),
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                1);
        Integer idReserva = reserva.getIdReserva();
        reservaService.cancelarPedidoReserva(idReserva);
        entityManager.clear();

        mockMvc.perform(get("/pedidos")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ENCARGADO"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString(
                        "/pedidos/" + idReserva + "/reactivar")));

        reservaService.reactivarPedidoReserva(idReserva);

        assertEquals("ACTIVA", jdbcTemplate.queryForObject(
                "SELECT estado_reserva FROM reserva WHERE id_reserva = ?",
                String.class,
                idReserva));
        assertEquals("ACTIVA", jdbcTemplate.queryForObject(
                "SELECT estado_detalle_reserva FROM reserva_detalle WHERE id_reserva = ?",
                String.class,
                idReserva));
        assertEquals(stockInicial - 1, jdbcTemplate.queryForObject(
                "SELECT stock FROM libro WHERE id_libro = ?",
                Integer.class,
                idLibro));
    }

    private String ultimoMovimiento(String entidad, Integer idRegistro) {
        return jdbcTemplate.queryForObject("""
                SELECT tipo_movimiento
                FROM auditoria_eliminacion_logica
                WHERE entidad = ? AND id_registro = ?
                ORDER BY id_auditoria DESC
                LIMIT 1
                """, String.class, entidad, idRegistro);
    }
}
