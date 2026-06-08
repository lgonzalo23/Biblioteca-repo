package com.luzdelsaber.biblioteca.controller;

import java.net.URI;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.luzdelsaber.biblioteca.dto.PerfilForm;
import com.luzdelsaber.biblioteca.exception.UsuarioValidationException;
import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class PerfilController {

    private final UsuarioService usuarioService;

    public PerfilController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/perfil")
    public String actualizarPerfil(
            @ModelAttribute PerfilForm form,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Object usuarioId = session.getAttribute("usuarioId");
        if (!(usuarioId instanceof Integer idUsuario)) {
            return "redirect:/login";
        }

        try {
            Usuario usuario = usuarioService.actualizarPerfil(idUsuario, form);
            session.setAttribute("usuarioNombre", usuario.getNombreCompleto());
            redirectAttributes.addFlashAttribute("perfilSuccessMessage", "Perfil actualizado correctamente.");
        } catch (UsuarioValidationException ex) {
            redirectAttributes.addFlashAttribute("perfilErrorMessages", ex.getErrores());
        }

        return "redirect:" + rutaRetorno(request);
    }

    private String rutaRetorno(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/inicio";
        }
        URI uri = URI.create(referer);
        String ruta = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/inicio" : uri.getRawPath();
        return uri.getRawQuery() == null ? ruta : ruta + "?" + uri.getRawQuery();
    }
}
