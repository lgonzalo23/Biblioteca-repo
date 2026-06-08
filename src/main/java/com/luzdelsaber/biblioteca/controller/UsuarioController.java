package com.luzdelsaber.biblioteca.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.luzdelsaber.biblioteca.dto.UsuarioForm;
import com.luzdelsaber.biblioteca.exception.UsuarioValidationException;
import com.luzdelsaber.biblioteca.service.UsuarioService;

import jakarta.validation.Valid;

@Controller
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public String usuarios(@RequestParam(name = "q", required = false) String termino, Model model) {
        cargarModelo(model, termino, new UsuarioForm(), List.of());
        return "usuarios";
    }

    @PostMapping("/usuarios")
    public String crear(
            @Valid @ModelAttribute("usuarioForm") UsuarioForm form,
            BindingResult result,
            @RequestParam(name = "q", required = false) String termino,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarModelo(model, termino, form, result.getFieldErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .toList());
            return "usuarios";
        }

        try {
            usuarioService.crear(form);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario registrado correctamente.");
            return "redirect:/usuarios";
        } catch (UsuarioValidationException ex) {
            cargarModelo(model, termino, form, ex.getErrores());
            return "usuarios";
        }
    }

    @PostMapping("/usuarios/{idUsuario}/actualizar")
    public String actualizar(
            @PathVariable Integer idUsuario,
            @ModelAttribute UsuarioForm form,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.actualizar(idUsuario, form);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario actualizado correctamente.");
        } catch (UsuarioValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{idUsuario}/baja")
    public String bajaLogica(@PathVariable Integer idUsuario, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarLogico(idUsuario);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario marcado como INACTIVO.");
        } catch (UsuarioValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{idUsuario}/eliminar")
    public String eliminarFisico(@PathVariable Integer idUsuario, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarFisico(idUsuario);
            redirectAttributes.addFlashAttribute("successMessage", "Usuario eliminado definitivamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessages",
                    List.of("No se pudo eliminar el usuario. Verifica que no tenga registros relacionados."));
        }
        return "redirect:/usuarios";
    }

    private void cargarModelo(Model model, String termino, UsuarioForm form, List<String> errores) {
        model.addAttribute("usuarios", usuarioService.listar(termino));
        model.addAttribute("roles", usuarioService.listarRoles());
        model.addAttribute("usuarioForm", form);
        model.addAttribute("q", termino);
        if (!errores.isEmpty()) {
            model.addAttribute("errorMessages", errores);
        }
    }
}
