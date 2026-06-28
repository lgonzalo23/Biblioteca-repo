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

import com.luzdelsaber.biblioteca.dto.AutorForm;
import com.luzdelsaber.biblioteca.dto.CategoriaForm;
import com.luzdelsaber.biblioteca.dto.LibroForm;
import com.luzdelsaber.biblioteca.exception.BibliografiaValidationException;
import com.luzdelsaber.biblioteca.service.BibliografiaService;

import jakarta.validation.Valid;

@Controller
public class LibroController {

    private final BibliografiaService bibliografiaService;

    public LibroController(BibliografiaService bibliografiaService) {
        this.bibliografiaService = bibliografiaService;
    }

    @GetMapping("/libros")
    public String libros(
            @RequestParam(name = "q", required = false) String termino,
            @RequestParam(name = "categoriaQ", required = false) String categoriaTermino,
            @RequestParam(name = "autorQ", required = false) String autorTermino,
            Model model) {
        cargarModelo(
                model,
                termino,
                categoriaTermino,
                autorTermino,
                new LibroForm(),
                new CategoriaForm(),
                new AutorForm(),
                List.of());
        return "libros";
    }

    @PostMapping("/libros")
    public String crearLibro(
            @Valid @ModelAttribute("libroForm") LibroForm form,
            BindingResult result,
            @RequestParam(name = "q", required = false) String termino,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarModelo(model, termino, form, new CategoriaForm(), new AutorForm(), erroresDesde(result));
            return "libros";
        }

        try {
            bibliografiaService.crearLibro(form);
            redirectAttributes.addFlashAttribute("successMessage", "Libro registrado correctamente.");
            return "redirect:/libros";
        } catch (BibliografiaValidationException ex) {
            cargarModelo(model, termino, form, new CategoriaForm(), new AutorForm(), ex.getErrores());
            return "libros";
        }
    }

    @PostMapping("/libros/{idLibro}/actualizar")
    public String actualizarLibro(
            @PathVariable Integer idLibro,
            @ModelAttribute LibroForm form,
            RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.actualizarLibro(idLibro, form);
            redirectAttributes.addFlashAttribute("successMessage", "Libro actualizado correctamente.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros";
    }

    @PostMapping("/libros/{idLibro}/baja")
    public String bajaLogicaLibro(@PathVariable Integer idLibro, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarLibroLogico(idLibro);
            redirectAttributes.addFlashAttribute("successMessage", "Libro marcado como NO_DISPONIBLE.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros";
    }

    @PostMapping("/libros/{idLibro}/eliminar")
    public String eliminarLibro(@PathVariable Integer idLibro, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarLibroFisico(idLibro);
            redirectAttributes.addFlashAttribute("successMessage", "Libro eliminado definitivamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessages",
                    List.of("No se pudo eliminar el libro. Verifica que no tenga registros relacionados."));
        }
        return "redirect:/libros";
    }

    @PostMapping("/libros/categorias")
    public String crearCategoria(
            @Valid @ModelAttribute("categoriaForm") CategoriaForm form,
            BindingResult result,
            @RequestParam(name = "q", required = false) String termino,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarModelo(model, termino, new LibroForm(), form, new AutorForm(), erroresDesde(result));
            return "libros";
        }

        try {
            bibliografiaService.crearCategoria(form);
            redirectAttributes.addFlashAttribute("successMessage", "Categoria registrada correctamente.");
            return "redirect:/libros#categorias";
        } catch (BibliografiaValidationException ex) {
            cargarModelo(model, termino, new LibroForm(), form, new AutorForm(), ex.getErrores());
            return "libros";
        }
    }

    @PostMapping("/libros/categorias/{idCategoria}/actualizar")
    public String actualizarCategoria(
            @PathVariable Integer idCategoria,
            @ModelAttribute CategoriaForm form,
            RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.actualizarCategoria(idCategoria, form);
            redirectAttributes.addFlashAttribute("successMessage", "Categoria actualizada correctamente.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros#categorias";
    }

    @PostMapping("/libros/categorias/{idCategoria}/baja")
    public String bajaLogicaCategoria(@PathVariable Integer idCategoria, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarCategoriaLogico(idCategoria);
            redirectAttributes.addFlashAttribute("successMessage", "Categoria marcada como INACTIVA.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros#categorias";
    }

    @PostMapping("/libros/categorias/{idCategoria}/eliminar")
    public String eliminarCategoria(@PathVariable Integer idCategoria, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarCategoriaFisico(idCategoria);
            redirectAttributes.addFlashAttribute("successMessage", "Categoria eliminada definitivamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessages",
                    List.of("No se pudo eliminar la categoria. Verifica que no tenga libros relacionados."));
        }
        return "redirect:/libros#categorias";
    }

    @PostMapping("/libros/autores")
    public String crearAutor(
            @Valid @ModelAttribute("autorForm") AutorForm form,
            BindingResult result,
            @RequestParam(name = "q", required = false) String termino,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            cargarModelo(model, termino, new LibroForm(), new CategoriaForm(), form, erroresDesde(result));
            return "libros";
        }

        try {
            bibliografiaService.crearAutor(form);
            redirectAttributes.addFlashAttribute("successMessage", "Autor registrado correctamente.");
            return "redirect:/libros#autores";
        } catch (BibliografiaValidationException ex) {
            cargarModelo(model, termino, new LibroForm(), new CategoriaForm(), form, ex.getErrores());
            return "libros";
        }
    }

    @PostMapping("/libros/autores/{idAutor}/actualizar")
    public String actualizarAutor(
            @PathVariable Integer idAutor,
            @ModelAttribute AutorForm form,
            RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.actualizarAutor(idAutor, form);
            redirectAttributes.addFlashAttribute("successMessage", "Autor actualizado correctamente.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros#autores";
    }

    @PostMapping("/libros/autores/{idAutor}/baja")
    public String bajaLogicaAutor(@PathVariable Integer idAutor, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarAutorLogico(idAutor);
            redirectAttributes.addFlashAttribute("successMessage", "Autor marcado como INACTIVO.");
        } catch (BibliografiaValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessages", ex.getErrores());
        }
        return "redirect:/libros#autores";
    }

    @PostMapping("/libros/autores/{idAutor}/eliminar")
    public String eliminarAutor(@PathVariable Integer idAutor, RedirectAttributes redirectAttributes) {
        try {
            bibliografiaService.eliminarAutorFisico(idAutor);
            redirectAttributes.addFlashAttribute("successMessage", "Autor eliminado definitivamente.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessages",
                    List.of("No se pudo eliminar el autor. Verifica que no tenga libros relacionados."));
        }
        return "redirect:/libros#autores";
    }

    private void cargarModelo(
            Model model,
            String termino,
            LibroForm libroForm,
            CategoriaForm categoriaForm,
            AutorForm autorForm,
            List<String> errores) {
        cargarModelo(model, termino, null, null, libroForm, categoriaForm, autorForm, errores);
    }

    private void cargarModelo(
            Model model,
            String termino,
            String categoriaTermino,
            String autorTermino,
            LibroForm libroForm,
            CategoriaForm categoriaForm,
            AutorForm autorForm,
            List<String> errores) {
        model.addAttribute("libros", bibliografiaService.listarLibros(termino));
        model.addAttribute("categorias", bibliografiaService.listarCategorias(categoriaTermino));
        model.addAttribute("categoriasActivas", bibliografiaService.listarCategoriasActivas());
        model.addAttribute("autores", bibliografiaService.listarAutores(autorTermino));
        model.addAttribute("autoresActivos", bibliografiaService.listarAutoresActivos());
        model.addAttribute("estadosLibro", List.of("DISPONIBLE", "PRESTADO", "RESERVADO", "NO_DISPONIBLE"));
        model.addAttribute("estadosRegistro", List.of("ACTIVO", "INACTIVO"));
        model.addAttribute("libroForm", libroForm);
        model.addAttribute("categoriaForm", categoriaForm);
        model.addAttribute("autorForm", autorForm);
        model.addAttribute("q", termino);
        model.addAttribute("categoriaQ", categoriaTermino);
        model.addAttribute("autorQ", autorTermino);
        if (!errores.isEmpty()) {
            model.addAttribute("errorMessages", errores);
        }
    }

    private List<String> erroresDesde(BindingResult result) {
        return result.getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .toList();
    }
}
