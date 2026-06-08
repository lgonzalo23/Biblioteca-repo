package com.luzdelsaber.biblioteca.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelController {

    private final UsuarioService usuarioService;

    public GlobalModelController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("usuarioActual")
    public Usuario usuarioActual(HttpSession session) {
        Object usuarioId = session.getAttribute("usuarioId");
        if (usuarioId instanceof Integer idUsuario) {
            return usuarioService.buscarPorId(idUsuario).orElse(null);
        }
        return null;
    }
}
