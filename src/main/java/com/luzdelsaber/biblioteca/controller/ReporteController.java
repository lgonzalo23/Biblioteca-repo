package com.luzdelsaber.biblioteca.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.luzdelsaber.biblioteca.service.ReporteService;

@Controller
public class ReporteController {

    private static final List<String> MESES = List.of(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre");

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model) {
        LocalDate fechaActual = LocalDate.now();
        int anioActual = fechaActual.getYear();
        model.addAttribute("meses", MESES);
        model.addAttribute("mesActual", MESES.get(fechaActual.getMonthValue() - 1));
        model.addAttribute("anioActual", anioActual);
        model.addAttribute("aniosDisponibles",
                IntStream.rangeClosed(2025, Math.max(2028, anioActual + 2)).boxed().toList());
        model.addAttribute("fechaInicioRango", fechaActual.withDayOfMonth(1));
        model.addAttribute("fechaFinRango", fechaActual);
        model.addAttribute("fechaActual", fechaActual);
        return "reportes";
    }

    @GetMapping("/admin/reporte/prestamos-rango/descargar")
    public ResponseEntity<byte[]> descargarReportePrestamosPorRango(
            @RequestParam("fechaInicio")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam("fechaFin")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        byte[] pdf = reporteService.generarReportePrestamosPorRango(fechaInicio, fechaFin);
        String nombreArchivo = "ReportePrestamos-"
                + fechaInicio + "-a-" + fechaFin + ".pdf";

        return crearDescarga(pdf, nombreArchivo);
    }

    @GetMapping("/pedidos/{idReserva}/comprobante")
    public ResponseEntity<byte[]> descargarComprobantePrestamo(
            @PathVariable Integer idReserva) {
        byte[] pdf = reporteService.generarComprobantePrestamo(idReserva);
        return crearDescarga(pdf, String.format("ComprobantePrestamo-%06d.pdf", idReserva));
    }

    @GetMapping("/admin/reporte/eliminaciones-logicas/descargar")
    public ResponseEntity<byte[]> descargarReporteEliminacionesLogicas(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteEliminacionesLogicas(mes, anio),
                "ReporteEliminacionesLogicas", mes, anio);
    }

    @GetMapping("/admin/reporte/stock/descargar")
    public ResponseEntity<byte[]> descargarReporteStock(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteStock(mes, anio),
                "ReporteStock", mes, anio);
    }

    @GetMapping("/admin/reporte/descargar")
    public ResponseEntity<byte[]> descargarReporteTiempoPromedio(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteTiempoPromedio(mes, anio),
                "ReporteTiempoPromedio", mes, anio);
    }

    @GetMapping("/admin/reporte/libros-mas-solicitados/descargar")
    public ResponseEntity<byte[]> descargarReporteLibrosMasSolicitados(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteLibrosMasSolicitados(mes, anio),
                "ReporteLibrosMasSolicitados", mes, anio);
    }

    @GetMapping("/admin/reporte/horario-demanda/descargar")
    public ResponseEntity<byte[]> descargarReporteHorarioMayorDemanda(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteHorarioMayorDemanda(mes, anio),
                "ReporteHorarioMayorDemanda", mes, anio);
    }

    @GetMapping("/admin/reporte/devoluciones-tardias/descargar")
    public ResponseEntity<byte[]> descargarReporteDevolucionesTardias(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReportePorcentajeDevolucionesTardias(mes, anio),
                "ReportePorcentajeDevolucionesTardias", mes, anio);
    }

    @GetMapping("/admin/reporte/tasa-incidencias/descargar")
    public ResponseEntity<byte[]> descargarReporteTasaIncidencias(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteTasaIncidencias(mes, anio),
                "ReportePorcentajeIncidenciasMensuales", mes, anio);
    }

    @GetMapping("/admin/reporte/promedio-usuario/descargar")
    public ResponseEntity<byte[]> descargarReportePromedioPorUsuario(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReportePromedioPrestamosPorUsuario(mes, anio),
                "ReportePromedioPrestamosPorUsuario", mes, anio);
    }

    @GetMapping("/admin/reporte/estadisticas-prestamos/descargar")
    public ResponseEntity<byte[]> descargarReporteEstadisticasPrestamos(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        return crearDescarga(
                reporteService.generarReporteEstadisticasPrestamos(mes, anio),
                "ReporteEstadisticasPrestamos", mes, anio);
    }

    @GetMapping("/admin/reporte/indicador/descargar")
    public ResponseEntity<byte[]> descargarReporteIndicador(
            @RequestParam("tipo") String tipo,
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        IndicadorGenerado indicador = switch (tipo.trim().toUpperCase()) {
            case "TIEMPO_PROMEDIO" -> new IndicadorGenerado(
                    reporteService.generarReporteTiempoPromedio(mes, anio),
                    "IndicadorTiempoPromedio");
            case "DEVOLUCIONES_TARDIAS" -> new IndicadorGenerado(
                    reporteService.generarReportePorcentajeDevolucionesTardias(mes, anio),
                    "IndicadorDevolucionesTardias");
            case "TASA_INCIDENCIAS" -> new IndicadorGenerado(
                    reporteService.generarReporteTasaIncidencias(mes, anio),
                    "IndicadorPorcentajeIncidenciasMensuales");
            case "LIBRO_MAS_SOLICITADO" -> new IndicadorGenerado(
                    reporteService.generarReporteLibrosMasSolicitados(mes, anio),
                    "IndicadorLibroMasSolicitado");
            case "HORARIO_DEMANDA" -> new IndicadorGenerado(
                    reporteService.generarReporteHorarioMayorDemanda(mes, anio),
                    "IndicadorHorarioMayorDemanda");
            case "PROMEDIO_USUARIO" -> new IndicadorGenerado(
                    reporteService.generarReportePromedioPrestamosPorUsuario(mes, anio),
                    "IndicadorPromedioPorUsuario");
            default -> throw new IllegalArgumentException("El indicador seleccionado no es válido.");
        };

        return crearDescarga(indicador.pdf(), indicador.nombreArchivo(), mes, anio);
    }

    private ResponseEntity<byte[]> crearDescarga(byte[] pdf, String prefijo, String mes, String anio) {
        String mesArchivo = textoOValorActual(mes, MESES.get(LocalDate.now().getMonthValue() - 1));
        String anioArchivo = textoOValorActual(anio, String.valueOf(LocalDate.now().getYear()));
        String nombreArchivo = prefijo + "-" + mesArchivo + "-" + anioArchivo + ".pdf";

        return crearDescarga(pdf, nombreArchivo);
    }

    private ResponseEntity<byte[]> crearDescarga(byte[] pdf, String nombreArchivo) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombreArchivo, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(pdf);
    }

    private String textoOValorActual(String valor, String valorActual) {
        return valor == null || valor.isBlank() ? valorActual : valor.trim();
    }

    private record IndicadorGenerado(byte[] pdf, String nombreArchivo) {
    }
}
