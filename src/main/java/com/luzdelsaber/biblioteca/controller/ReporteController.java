package com.luzdelsaber.biblioteca.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.luzdelsaber.biblioteca.service.ReporteService;

@Controller
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model) {
        model.addAttribute("mesActual", obtenerMesActual());
        model.addAttribute("anioActual", java.time.LocalDate.now().getYear());
        return "reportes";
    }

    @GetMapping("/admin/reporte/descargar")
    @ResponseBody
    public ResponseEntity<byte[]> descargarReporte(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        
        byte[] pdfBytes = reporteService.generarReporteTiempoPromedio(mes, anio);

        String mesNombre = (mes != null && !mes.trim().isEmpty()) ? mes.trim() : "Mes";
        String filename = "ReporteTiempoPromedio-" + mesNombre + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/admin/reporte/libros-mas-solicitados/descargar")
    @ResponseBody
    public ResponseEntity<byte[]> descargarReporteLibrosMasSolicitados(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        
        byte[] pdfBytes = reporteService.generarReporteLibrosMasSolicitados(mes, anio);

        String mesNombre = (mes != null && !mes.trim().isEmpty()) ? mes.trim() : "Mes";
        String filename = "ReporteLibrosMasSolicitados-" + mesNombre + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/admin/reporte/horario-demanda/descargar")
    @ResponseBody
    public ResponseEntity<byte[]> descargarReporteHorarioMayorDemanda(
            @RequestParam(name = "mes", required = false) String mes,
            @RequestParam(name = "anio", required = false) String anio) {
        
        byte[] pdfBytes = reporteService.generarReporteHorarioMayorDemanda(mes, anio);

        String mesNombre = (mes != null && !mes.trim().isEmpty()) ? mes.trim() : "Mes";
        String filename = "ReporteHorarioMayorDemanda-" + mesNombre + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(pdfBytes.length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private String obtenerMesActual() {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", 
                          "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        int mes = java.time.LocalDate.now().getMonthValue();
        return meses[mes - 1];
    }
}
