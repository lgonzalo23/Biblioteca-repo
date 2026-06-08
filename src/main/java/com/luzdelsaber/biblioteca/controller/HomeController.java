package com.luzdelsaber.biblioteca.controller;

import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.luzdelsaber.biblioteca.service.BibliografiaService;

@Controller
public class HomeController {

    private final BibliografiaService bibliografiaService;

    public HomeController(BibliografiaService bibliografiaService) {
        this.bibliografiaService = bibliografiaService;
    }

    @GetMapping("/")
    public String index(@RequestParam(name = "q", required = false) String termino, Model model) {
        model.addAttribute("librosDestacados", bibliografiaService.listarLibrosCatalogo(termino)
                .stream()
                .limit(3)
                .toList());
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
        model.addAttribute("categoriaSeleccionada", categoriaId);
        model.addAttribute("q", termino);
        return "inicio";
    }

    @GetMapping("/reservas")
    public String reservas() {
        return "reservas";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/pedidos")
    public String pedidos() {
        return "pedidos";
    }

}
