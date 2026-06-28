package com.luzdelsaber.biblioteca.controller;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(HttpSession session) {
        if (session.getAttribute("usuarioId") != null) {
            return "redirect:" + rutaInicialPorRol(String.valueOf(session.getAttribute("usuarioRol")));
        }
        return "login";
    }

    @PostMapping("/auth/login")
    public String iniciarSesion(
            @RequestParam("username") String correo,
            @RequestParam("password") String contrasena,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        return usuarioService.buscarPorCredenciales(correo, contrasena)
                .map(usuario -> usuarioService.obtenerMensajeBloqueoLogin(usuario)
                        .map(mensaje -> {
                            redirectAttributes.addFlashAttribute("loginError", mensaje);
                            return "redirect:/login";
                        })
                        .orElseGet(() -> abrirSesion(usuario, session)))
                .orElse("redirect:/login?error");
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    @PostMapping("/logout")
    public String cerrarSesionPost(HttpSession session) {
        session.invalidate();
        return "redirect:/login?logout";
    }

    private String abrirSesion(Usuario usuario, HttpSession session) {
        String rol = normalizarRol(usuario.getRol().getNombreRol());
        session.setAttribute("usuarioId", usuario.getIdUsuario());
        session.setAttribute("usuarioNombre", usuario.getNombreCompleto());
        session.setAttribute("usuarioRol", rol);
        return "redirect:" + rutaInicialPorRol(rol);
    }

    private String rutaInicialPorRol(String rol) {
        String rolNormalizado = normalizarRol(rol);
        return switch (rolNormalizado) {
            case "ADMINISTRADOR", "ADMIN", "ADMINISTRADOR/A", "ADMINISTRADORA", "ENCARGADO", "ENCARGADA" -> "/pedidos";
            case "PRESTATARIO", "PRESTATARIA" -> "/inicio";
            default -> "/login";
        };
    }

    private String normalizarRol(String rol) {
        if (rol == null) {
            return "";
        }
        String rolNormalizado = rol.trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");

        return switch (rolNormalizado) {
            case "ADMIN", "ADMINISTRADOR/A", "ADMINISTRADORA" -> "ADMINISTRADOR";
            case "ENCARGADA" -> "ENCARGADO";
            case "PRESTATARIA" -> "PRESTATARIO";
            default -> rolNormalizado;
        };
    }
}
