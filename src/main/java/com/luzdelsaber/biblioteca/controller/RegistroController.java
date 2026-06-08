package com.luzdelsaber.biblioteca.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.luzdelsaber.biblioteca.dto.UsuarioForm;
import com.luzdelsaber.biblioteca.exception.UsuarioValidationException;
import com.luzdelsaber.biblioteca.service.UsuarioService;

@Controller
public class RegistroController {

    private final UsuarioService usuarioService;

    public RegistroController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        if (!model.containsAttribute("usuarioForm")) {
            model.addAttribute("usuarioForm", new UsuarioForm());
        }
        return "registro";
    }

    @PostMapping("/register")
    public String registrar(
            @ModelAttribute("usuarioForm") UsuarioForm form,
            Model model) {
        try {
            usuarioService.registrarPublico(form);
            return "redirect:/login?registered";
        } catch (UsuarioValidationException ex) {
            model.addAttribute("errorMessages", ex.getErrores());
            return "registro";
        } catch (RuntimeException ex) {
            model.addAttribute("errorMessages", List.of("No se pudo completar el registro."));
            return "registro";
        }
    }
}
