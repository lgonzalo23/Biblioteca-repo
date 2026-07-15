package com.luzdelsaber.biblioteca;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import static org.hamcrest.Matchers.containsString;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.luzdelsaber.biblioteca.service.ReporteService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;

@SpringBootTest
@AutoConfigureMockMvc
class ReporteServiceIntegrationTests {

    private static final Path DIRECTORIO_REPORTES = Path.of("target", "reportes-verificacion");

    @Autowired
    private ReporteService reporteService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void muestraLaPantallaDeReportesParaElAdministrador() throws Exception {
        mockMvc.perform(get("/admin/reportes")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Administrador de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(view().name("reportes"))
                .andExpect(content().string(containsString("Reportes Administrativos")))
                .andExpect(content().string(containsString("Tiempo Promedio de Préstamo")))
                .andExpect(content().string(containsString("Porcentaje de Devoluciones Tardías")))
                .andExpect(content().string(containsString("Porcentaje de Incidencias Mensuales")))
                .andExpect(content().string(containsString("Libro Más Solicitado")))
                .andExpect(content().string(containsString("Horario de Mayor Demanda")))
                .andExpect(content().string(containsString("Promedio de Préstamos por Usuario")))
                .andExpect(content().string(containsString("Estadísticas de Préstamos")))
                .andExpect(content().string(containsString("Préstamos por rango de fechas")))
                .andExpect(content().string(containsString("Eliminaciones lógicas")))
                .andExpect(content().string(containsString("Reporte de stock")));
    }

    @Test
    void generaLosOnceReportesEnFormatoPdf() throws IOException {
        verificarPdf(
                reporteService.generarReporteTiempoPromedio("Junio", "2026"),
                "reporte-tiempo-promedio.pdf");
        verificarPdf(
                reporteService.generarReporteLibrosMasSolicitados("Junio", "2026"),
                "reporte-libros-mas-solicitados.pdf");
        verificarPdf(
                reporteService.generarReporteHorarioMayorDemanda("Junio", "2026"),
                "reporte-horario-demanda.pdf");
        verificarPdf(
                reporteService.generarReportePorcentajeDevolucionesTardias("Junio", "2026"),
                "reporte-devoluciones-tardias.pdf");
        verificarPdf(
                reporteService.generarReporteTasaIncidencias("Junio", "2026"),
                "reporte-porcentaje-incidencias-mensuales.pdf");
        verificarPdf(
                reporteService.generarReportePromedioPrestamosPorUsuario("Junio", "2026"),
                "reporte-promedio-usuario.pdf");
        verificarPdf(
                reporteService.generarReporteEstadisticasPrestamos("Junio", "2026"),
                "reporte-estadisticas-prestamos.pdf");
        verificarPdf(
                reporteService.generarReportePrestamosPorRango(
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
                "reporte-prestamos-rango.pdf");
        verificarPdf(
                reporteService.generarComprobantePrestamo(obtenerIdReservaConPrestamo()),
                "comprobante-prestamo.pdf");
        verificarPdf(
                reporteService.generarReporteEliminacionesLogicas("Junio", "2026"),
                "reporte-eliminaciones-logicas.pdf");
        verificarPdf(
                reporteService.generarReporteStock("Junio", "2026"),
                "reporte-stock.pdf");
    }

    @Test
    void generaElReporteDeLibrosAunqueElPeriodoNoTengaPrestamos() throws IOException {
        byte[] pdf = reporteService.generarReporteLibrosMasSolicitados("Diciembre", "2100");
        verificarPdf(pdf, "reporte-libros-mas-solicitados-sin-datos.pdf");

        PdfReader lector = new PdfReader(pdf);
        try {
            String texto = new PdfTextExtractor(lector).getTextFromPage(1);
            assertTrue(texto.contains("Reporte de Libros Más Solicitados"));
            assertTrue(texto.contains("Resumen de Indicadores"));
            assertTrue(texto.contains("Libro Más Solicitado"));
            assertTrue(texto.contains("Sin datos"));
        } finally {
            lector.close();
        }
    }

    @Test
    void generaElReporteDeHorarioAunqueElPeriodoNoTengaPrestamos() throws IOException {
        byte[] pdf = reporteService.generarReporteHorarioMayorDemanda("Diciembre", "2100");
        verificarPdf(pdf, "reporte-horario-demanda-sin-datos.pdf");

        PdfReader lector = new PdfReader(pdf);
        try {
            String texto = new PdfTextExtractor(lector).getTextFromPage(1);
            assertTrue(texto.contains("Reporte de Horario de Mayor Demanda de Préstamos"));
            assertTrue(texto.contains("Resumen de Indicadores"));
            assertTrue(texto.contains("Horario de Mayor Demanda"));
            assertTrue(texto.contains("N/A"));
        } finally {
            lector.close();
        }
    }

    @Test
    void descargaElIndicadorSeleccionadoDesdeElFormularioUnificado() throws Exception {
        mockMvc.perform(get("/admin/reporte/indicador/descargar")
                        .param("tipo", "TASA_INCIDENCIAS")
                        .param("mes", "Junio")
                        .param("anio", "2026")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Administrador de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        containsString("IndicadorPorcentajeIncidenciasMensuales-Junio-2026.pdf")));
    }

    @Test
    void descargaLosPrestamosDelRangoSeleccionado() throws Exception {
        mockMvc.perform(get("/admin/reporte/prestamos-rango/descargar")
                        .param("fechaInicio", "2026-06-01")
                        .param("fechaFin", "2026-06-30")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Administrador de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void permiteAlEncargadoDescargarElComprobanteDelPrestamo() throws Exception {
        Integer idReserva = obtenerIdReservaConPrestamo();

        mockMvc.perform(get("/pedidos/{idReserva}/comprobante", idReserva)
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Encargado de prueba")
                        .sessionAttr("usuarioRol", "ENCARGADO"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void descargaElReporteDeEliminacionesLogicas() throws Exception {
        mockMvc.perform(get("/admin/reporte/eliminaciones-logicas/descargar")
                        .param("mes", "Junio")
                        .param("anio", "2026")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Administrador de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void descargaElReporteDeStock() throws Exception {
        mockMvc.perform(get("/admin/reporte/stock/descargar")
                        .param("mes", "Junio")
                        .param("anio", "2026")
                        .sessionAttr("usuarioId", 0)
                        .sessionAttr("usuarioNombre", "Administrador de prueba")
                        .sessionAttr("usuarioRol", "ADMINISTRADOR"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void reporteDeStockNoMuestraMetricasRetiradas() throws IOException {
        byte[] pdf = reporteService.generarReporteStock("Junio", "2026");
        verificarPdf(pdf, "reporte-stock-simplificado.pdf");

        PdfReader lector = new PdfReader(pdf);
        try {
            String texto = new PdfTextExtractor(lector).getTextFromPage(1);
            assertTrue(texto.contains("Reporte de Stock"));
            assertTrue(texto.contains("Total de ejemplares"));
            assertFalse(texto.contains("Ejemplares disponibles"));
            assertFalse(texto.contains("Ejemplares comprometidos"));
            assertFalse(texto.contains("Préstamos del mes"));
            assertFalse(texto.contains("Títulos no disponibles"));
            assertFalse(texto.contains("Estado"));
        } finally {
            lector.close();
        }
    }

    private Integer obtenerIdReservaConPrestamo() {
        Integer idReserva = jdbcTemplate.queryForObject("""
                SELECT MIN(id_reserva)
                FROM prestamo
                WHERE id_reserva IS NOT NULL
                """, Integer.class);
        assertNotNull(idReserva, "Debe existir al menos una reserva convertida en préstamo");
        return idReserva;
    }

    private void verificarPdf(byte[] contenido, String nombreArchivo) throws IOException {
        assertTrue(contenido.length > 1_000, "El PDF generado no debe estar vacío");
        assertArrayEquals(
                "%PDF".getBytes(StandardCharsets.US_ASCII),
                Arrays.copyOf(contenido, 4),
                "La descarga debe tener una cabecera PDF válida");

        Files.createDirectories(DIRECTORIO_REPORTES);
        Files.write(DIRECTORIO_REPORTES.resolve(nombreArchivo), contenido);
    }
}
