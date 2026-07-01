package com.luzdelsaber.biblioteca.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.luzdelsaber.biblioteca.exception.ReservaValidationException;
import com.luzdelsaber.biblioteca.service.BibliografiaService;
import com.luzdelsaber.biblioteca.service.DashboardService;
import com.luzdelsaber.biblioteca.service.IncidenciaService;
import com.luzdelsaber.biblioteca.service.ReservaService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final BibliografiaService bibliografiaService;
    private final ReservaService reservaService;
    private final IncidenciaService incidenciaService;
    private final DashboardService dashboardService;

    public HomeController(
            BibliografiaService bibliografiaService,
            ReservaService reservaService,
            IncidenciaService incidenciaService,
            DashboardService dashboardService) {
        this.bibliografiaService = bibliografiaService;
        this.reservaService = reservaService;
        this.incidenciaService = incidenciaService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String index(@RequestParam(name = "q", required = false) String termino, Model model) {
        model.addAttribute("librosDestacados", bibliografiaService.listarLibrosCatalogo(termino));
        model.addAttribute("q", termino);
        return "index";
    }

    @GetMapping("/inicio")
    public String inicio(
            @RequestParam(name = "q", required = false) String termino,
            @RequestParam(name = "categoria", required = false) Integer categoriaId,
            Model model) {
        model.addAttribute("libros", bibliografiaService.listarLibrosCatalogo(termino, categoriaId));
        model.addAttribute("categorias", bibliografiaService.listarCategoriasActivas());
        model.addAttribute("horariosReserva", horariosReserva());
        model.addAttribute("categoriaSeleccionada", categoriaId);
        model.addAttribute("q", termino);
        return "inicio";
    }

    @GetMapping("/reservas")
    public String reservas(
            @RequestParam(name = "q", required = false) String termino,
            HttpSession session,
            Model model) {
        Integer idUsuario = obtenerIdUsuario(session);
        model.addAttribute("reservas", reservaService.listarReservas(idUsuario, termino));
        model.addAttribute("horariosReserva", horariosReserva());
        model.addAttribute("q", termino);
        return "reservas";
    }

    @PostMapping("/reservas/crear")
    public String crearReserva(
            @RequestParam(name = "idLibro", required = false) List<Integer> idsLibro,
            @RequestParam("fechaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaReserva,
            @RequestParam("horaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaReserva,
            @RequestParam("horasPrestamo") Integer horasPrestamo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.crearReserva(obtenerIdUsuario(session), idsLibro, fechaReserva, horaReserva, horasPrestamo);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva registrada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/inicio";
    }

    @PostMapping("/reservas/{idReserva}/actualizar")
    public String actualizarReserva(
            @PathVariable Integer idReserva,
            @RequestParam("fechaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaReserva,
            @RequestParam("horaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaReserva,
            @RequestParam("horasPrestamo") Integer horasPrestamo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.actualizarReserva(obtenerIdUsuario(session), idReserva, fechaReserva, horaReserva, horasPrestamo);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva actualizada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/reservas";
    }

    @PostMapping("/reservas/{idReserva}/cancelar")
    public String cancelarReserva(
            @PathVariable Integer idReserva,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelarReserva(obtenerIdUsuario(session), idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/reservas";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.obtenerDashboard());
        return "dashboard";
    }

    @GetMapping("/pedidos")
    public String pedidos(
            @RequestParam(name = "q", required = false) String termino,
        Model model) {
        model.addAttribute("reservas", reservaService.listarPedidos(termino));
        model.addAttribute("horariosReserva", horariosReserva());
        model.addAttribute("q", termino);
        return "pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/prestar")
    public String convertirReservaEnPrestamo(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.convertirReservaEnPrestamo(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva convertida en prestamo correctamente.");
            redirectAttributes.addFlashAttribute("idReservaPrestada", idReserva);
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/cancelar-prestamo")
    public String cancelarPrestamo(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelarPrestamo(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Prestamo cancelado correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/cancelar")
    public String cancelarPedidoReserva(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.cancelarPedidoReserva(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva cancelada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/reactivar")
    public String reactivarPedidoReserva(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.reactivarPedidoReserva(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva reactivada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/eliminar")
    public String eliminarPedidoReserva(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.eliminarPedidoReservaFisico(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva eliminada definitivamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/postergar")
    public String postergarPrestamo(
            @PathVariable Integer idReserva,
            @RequestParam("horasPrestamo") Integer horasPrestamo,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.postergarPrestamo(idReserva, horasPrestamo);
            redirectAttributes.addFlashAttribute("successMessage", "Prestamo postergado correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/postergar-reserva")
    public String postergarReservaDesdePedidos(
            @PathVariable Integer idReserva,
            @RequestParam("fechaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaReserva,
            @RequestParam("horaReserva") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaReserva,
            @RequestParam("horasPrestamo") Integer horasPrestamo,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.postergarReservaDesdePedidos(idReserva, fechaReserva, horaReserva, horasPrestamo);
            redirectAttributes.addFlashAttribute("successMessage", "Reserva postergada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/devolucion")
    public String registrarDevolucion(
            @PathVariable Integer idReserva,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.registrarDevolucion(idReserva);
            redirectAttributes.addFlashAttribute("successMessage", "Devolucion registrada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/{idReserva}/ampliar")
    public String ampliarPrestamo(
            @PathVariable Integer idReserva,
            @RequestParam("horasPrestamo") Integer horasPrestamo,
            RedirectAttributes redirectAttributes) {
        try {
            reservaService.ampliarPrestamo(idReserva, horasPrestamo);
            redirectAttributes.addFlashAttribute("successMessage", "Prestamo ampliado correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/incidencias")
    public String registrarIncidencia(
            @RequestParam("idPrestamo") Integer idPrestamo,
            @RequestParam("tipoIncidencia") String tipoIncidencia,
            @RequestParam(name = "descripcionIncidencia", required = false) String descripcionIncidencia,
            RedirectAttributes redirectAttributes) {
        try {
            boolean incidenciaRegistrada = reservaService.registrarIncidencia(idPrestamo, tipoIncidencia, descripcionIncidencia);
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    incidenciaRegistrada
                            ? "Incidencia registrada correctamente."
                            : "Prestamo revisado sin incidencia.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/pedidos";
    }

    @GetMapping("/incidencias")
    public String incidencias(
            @RequestParam(name = "q", required = false) String termino,
            Model model) {
        model.addAttribute("incidencias", incidenciaService.listarIncidencias(termino));
        model.addAttribute("q", termino);
        return "incidencias";
    }

    @PostMapping("/incidencias/{idIncidencia}/sancionar")
    public String aplicarSancion(
            @PathVariable Integer idIncidencia,
            @RequestParam("tipoSancion") String tipoSancion,
            @RequestParam(name = "diasSuspension", required = false) Integer diasSuspension,
            RedirectAttributes redirectAttributes) {
        try {
            incidenciaService.aplicarSancion(idIncidencia, tipoSancion, diasSuspension);
            redirectAttributes.addFlashAttribute("successMessage", "Sancion aplicada correctamente.");
        } catch (ReservaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/incidencias";
    }

    private Integer obtenerIdUsuario(HttpSession session) {
        Object usuarioId = session.getAttribute("usuarioId");
        if (usuarioId instanceof Integer idUsuario) {
            return idUsuario;
        }
        if (usuarioId instanceof Number numero) {
            return numero.intValue();
        }
        throw new ReservaValidationException(List.of("Debes iniciar sesion para reservar un libro."));
    }

    private List<LocalTime> horariosReserva() {
        List<LocalTime> horarios = new java.util.ArrayList<>();
        LocalTime hora = LocalTime.of(8, 0);
        LocalTime cierre = LocalTime.of(19, 0);

        while (!hora.isAfter(cierre)) {
            horarios.add(hora);
            hora = hora.plusMinutes(30);
        }

        return horarios;
    }

}
