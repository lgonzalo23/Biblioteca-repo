package com.luzdelsaber.biblioteca.service;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@Service
public class ReporteService {

    private final JdbcTemplate jdbcTemplate;

    public ReporteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public byte[] generarReporteTiempoPromedio(String mes, String anio) {
        try {
            if (mes == null || mes.trim().isEmpty()) {
                mes = obtenerMesActual();
            }
            if (anio == null || anio.trim().isEmpty()) {
                anio = String.valueOf(LocalDate.now().getYear());
            }

            int mesNum = obtenerNumeroMes(mes);
            int anioNum = Integer.parseInt(anio);
            String periodo = "Mensual- " + mes + " " + anio;

            // Calcular métricas desde la base de datos filtradas por mes y año
            Integer totalPrestamos = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM prestamo WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ?", 
                    Integer.class, mesNum, anioNum);
            if (totalPrestamos == null) {
                totalPrestamos = 0;
            }

            Long sumatoriaMinutos = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(TIME_TO_SEC(TIMEDIFF(hora_fin, hora_inicio)) / 60), 0) FROM prestamo WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ?", 
                    Long.class, mesNum, anioNum);
            if (sumatoriaMinutos == null) {
                sumatoriaMinutos = 0L;
            }

            Double tiempoPromedio = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(AVG(TIME_TO_SEC(TIMEDIFF(hora_fin, hora_inicio)) / 60), 0.0) FROM prestamo WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ?", 
                    Double.class, mesNum, anioNum);
            if (tiempoPromedio == null) {
                tiempoPromedio = 0.0;
            }

            // Redondear el tiempo promedio a 1 decimal
            tiempoPromedio = Math.round(tiempoPromedio * 10.0) / 10.0;

            // Ruta de la imagen del logotipo
            File logoFile = new File("src/main/resources/static/img/Logo_LuzdelSaber.png");
            String logoPath = logoFile.exists() ? logoFile.getAbsolutePath() : "";

            // Fecha de emisión formateada (dd-MM-yy)
            String fechaEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"));

            // Cargar y compilar plantilla
            InputStream reportStream = getClass().getResourceAsStream("/reports/reporte_tiempo_promedio.jrxml");
            if (reportStream == null) {
                throw new RuntimeException("No se encontró el archivo de reporte JRXML.");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Mapear parámetros
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("periodo", periodo);
            parameters.put("fechaEmision", fechaEmision);
            parameters.put("logoPath", logoPath);
            parameters.put("totalPrestamos", totalPrestamos);
            parameters.put("sumatoriaMinutos", sumatoriaMinutos);
            parameters.put("tiempoPromedio", tiempoPromedio);

            // Llenar y exportar a PDF
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte en PDF: " + e.getMessage(), e);
        }
    }

    public byte[] generarReporteLibrosMasSolicitados(String mes, String anio) {
        try {
            if (mes == null || mes.trim().isEmpty()) {
                mes = obtenerMesActual();
            }
            if (anio == null || anio.trim().isEmpty()) {
                anio = String.valueOf(LocalDate.now().getYear());
            }

            int mesNum = obtenerNumeroMes(mes);
            int anioNum = Integer.parseInt(anio);
            String periodo = "Mensual- " + mes + " " + anio;

            // 1. Total libros únicos solicitados
            Integer totalLibrosUnicos = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT id_libro) FROM prestamo WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ?", 
                    Integer.class, mesNum, anioNum);
            if (totalLibrosUnicos == null) {
                totalLibrosUnicos = 0;
            }

            // 2. Categoría más leída
            String categoriaMasLeida = "Ninguna";
            try {
                categoriaMasLeida = jdbcTemplate.queryForObject(
                        "SELECT c.nombre_categoria FROM prestamo p " +
                        "INNER JOIN libro l ON p.id_libro = l.id_libro " +
                        "INNER JOIN categoria c ON l.id_categoria = c.id_categoria " +
                        "WHERE MONTH(p.fecha_prestamo) = ? AND YEAR(p.fecha_prestamo) = ? " +
                        "GROUP BY c.id_categoria, c.nombre_categoria " +
                        "ORDER BY COUNT(*) DESC LIMIT 1", 
                        String.class, mesNum, anioNum);
                if (categoriaMasLeida == null) {
                    categoriaMasLeida = "Ninguna";
                }
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                categoriaMasLeida = "Ninguna";
            }

            // 3. Lista de libros (Top Demanda)
            String sqlLibros = 
                "SELECT l.titulo_libro AS titulo, " +
                "       COALESCE(GROUP_CONCAT(CONCAT(a.nombre_autor, ' ', a.apellido_autor) SEPARATOR ', '), 'Autor Desconocido') AS autor, " +
                "       c.nombre_categoria AS categoria, " +
                "       COUNT(*) AS cantidad " +
                "FROM prestamo p " +
                "INNER JOIN libro l ON p.id_libro = l.id_libro " +
                "INNER JOIN categoria c ON l.id_categoria = c.id_categoria " +
                "LEFT JOIN libro_autor la ON l.id_libro = la.id_libro " +
                "LEFT JOIN autor a ON la.id_autor = a.id_autor " +
                "WHERE MONTH(p.fecha_prestamo) = ? AND YEAR(p.fecha_prestamo) = ? " +
                "GROUP BY l.id_libro, l.titulo_libro, c.nombre_categoria " +
                "ORDER BY cantidad DESC, l.titulo_libro ASC";

            List<Map<String, Object>> librosList = jdbcTemplate.queryForList(sqlLibros, mesNum, anioNum);

            // Ruta de la imagen del logotipo
            File logoFile = new File("src/main/resources/static/img/Logo_LuzdelSaber.png");
            String logoPath = logoFile.exists() ? logoFile.getAbsolutePath() : "";

            // Fecha de emisión formateada (dd-MM-yy)
            String fechaEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"));

            // Cargar y compilar plantilla
            InputStream reportStream = getClass().getResourceAsStream("/reports/reporte_libros_mas_solicitados.jrxml");
            if (reportStream == null) {
                throw new RuntimeException("No se encontró el archivo de reporte de libros más solicitados JRXML.");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Mapear parámetros
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("periodo", periodo);
            parameters.put("fechaEmision", fechaEmision);
            parameters.put("logoPath", logoPath);
            parameters.put("totalLibrosUnicos", totalLibrosUnicos);
            parameters.put("categoriaMasLeida", categoriaMasLeida);

            // Crear DataSource con la lista de libros obtenidos
            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = 
                    new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(librosList);

            // Llenar y exportar a PDF
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte de libros más solicitados en PDF: " + e.getMessage(), e);
        }
    }

    public byte[] generarReporteHorarioMayorDemanda(String mes, String anio) {
        try {
            if (mes == null || mes.trim().isEmpty()) {
                mes = obtenerMesActual();
            }
            if (anio == null || anio.trim().isEmpty()) {
                anio = String.valueOf(LocalDate.now().getYear());
            }

            int mesNum = obtenerNumeroMes(mes);
            int anioNum = Integer.parseInt(anio);
            String periodo = "Mensual- " + mes + " " + anio;

            // 1. Total préstamos del periodo
            Integer totalPrestamos = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM prestamo WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ?", 
                    Integer.class, mesNum, anioNum);
            if (totalPrestamos == null) {
                totalPrestamos = 0;
            }

            // 2. Agrupación por horas
            String sqlHorarios = 
                "SELECT HOUR(hora_inicio) AS hora_num, " +
                "       COUNT(*) AS cantidad_prestamos " +
                "FROM prestamo " +
                "WHERE MONTH(fecha_prestamo) = ? AND YEAR(fecha_prestamo) = ? " +
                "GROUP BY HOUR(hora_inicio) " +
                "ORDER BY HOUR(hora_inicio) ASC";

            List<Map<String, Object>> rawRows = jdbcTemplate.queryForList(sqlHorarios, mesNum, anioNum);

            // Calcular porcentajes y determinar el pico
            String horarioPico = "N/A";
            long cantidadPico = 0;
            List<Map<String, Object>> processedRows = new java.util.ArrayList<>();
            for (Map<String, Object> row : rawRows) {
                int horaNum = ((Number) row.get("hora_num")).intValue();
                long count = ((Number) row.get("cantidad_prestamos")).longValue();
                
                String range = String.format("%02d:00 - %02d:00", horaNum, horaNum + 1);
                
                if (count > cantidadPico) {
                    cantidadPico = count;
                    horarioPico = range;
                }
                double percentage = totalPrestamos > 0 ? (count * 100.0 / totalPrestamos) : 0.0;
                percentage = Math.round(percentage * 10.0) / 10.0; // Redondear a 1 decimal

                Map<String, Object> newRow = new HashMap<>();
                newRow.put("rango_hora", range);
                newRow.put("cantidad", count);
                newRow.put("porcentaje", percentage);
                processedRows.add(newRow);
            }

            // Ruta de la imagen del logotipo
            File logoFile = new File("src/main/resources/static/img/Logo_LuzdelSaber.png");
            String logoPath = logoFile.exists() ? logoFile.getAbsolutePath() : "";

            // Fecha de emisión formateada (dd-MM-yy)
            String fechaEmision = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yy"));

            // Cargar y compilar plantilla
            InputStream reportStream = getClass().getResourceAsStream("/reports/reporte_horario_demanda.jrxml");
            if (reportStream == null) {
                throw new RuntimeException("No se encontró el archivo de reporte de horario de demanda JRXML.");
            }
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

            // Mapear parámetros
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("periodo", periodo);
            parameters.put("fechaEmision", fechaEmision);
            parameters.put("logoPath", logoPath);
            parameters.put("totalPrestamos", totalPrestamos);
            parameters.put("horarioPico", horarioPico);
            parameters.put("cantidadPico", cantidadPico);

            // Crear DataSource con la lista de filas procesadas
            net.sf.jasperreports.engine.data.JRBeanCollectionDataSource dataSource = 
                    new net.sf.jasperreports.engine.data.JRBeanCollectionDataSource(processedRows);

            // Llenar y exportar a PDF
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte de horario de mayor demanda en PDF: " + e.getMessage(), e);
        }
    }

    private int obtenerNumeroMes(String mes) {
        if (mes == null) return 0;
        return switch (mes.toLowerCase().trim()) {
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
            default -> 0;
        };
    }

    private String obtenerMesActual() {
        String[] meses = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", 
                          "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};
        int mes = LocalDate.now().getMonthValue();
        return meses[mes - 1];
    }
}
