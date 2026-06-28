package com.luzdelsaber.biblioteca.config;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AccesoInterceptor implements HandlerInterceptor {

    private static final Set<String> RUTAS_PUBLICAS = Set.of("/", "/login", "/registro", "/register", "/auth/login", "/error");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());

        if (esPublica(ruta)) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuarioId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        String rol = normalizarRol(session.getAttribute("usuarioRol"));
        if (!tienePermiso(ruta, rol)) {
            response.sendRedirect(request.getContextPath() + rutaInicialPorRol(rol) + "?sinPermiso");
            return false;
        }

        return true;
    }

    private boolean esPublica(String ruta) {
        return RUTAS_PUBLICAS.contains(ruta)
                || ruta.startsWith("/css/")
                || ruta.startsWith("/img/")
                || ruta.startsWith("/js/")
                || ruta.startsWith("/webjars/");
    }

    private boolean tienePermiso(String ruta, String rol) {
        if (esAdministrador(rol)) {
            return true;
        }
        if (esEncargado(rol)) {
            return ruta.startsWith("/pedidos")
                    || ruta.startsWith("/incidencias")
                    || ruta.startsWith("/libros")
                    || ruta.startsWith("/dashboard")
                    || ruta.startsWith("/perfil")
                    || ruta.startsWith("/logout");
        }
        if (esPrestatario(rol)) {
            return ruta.startsWith("/inicio")
                    || ruta.startsWith("/reservas")
                    || ruta.startsWith("/perfil")
                    || ruta.startsWith("/logout");
        }
        return ruta.startsWith("/logout");
    }

    private String rutaInicialPorRol(String rol) {
        if (esAdministrador(rol) || esEncargado(rol)) {
            return "/pedidos";
        }
        if (esPrestatario(rol)) {
            return "/inicio";
        }
        return "/login";
    }

    private String normalizarRol(Object rol) {
        if (rol == null) {
            return "";
        }
        return String.valueOf(rol)
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(" ", "_")
                .replace("-", "_");
    }

    private boolean esAdministrador(String rol) {
        return "ADMINISTRADOR".equals(rol)
                || "ADMIN".equals(rol)
                || "ADMINISTRADOR/A".equals(rol)
                || "ADMINISTRADORA".equals(rol);
    }

    private boolean esEncargado(String rol) {
        return "ENCARGADO".equals(rol)
                || "ENCARGADA".equals(rol);
    }

    private boolean esPrestatario(String rol) {
        return "PRESTATARIO".equals(rol)
                || "PRESTATARIA".equals(rol);
    }
}
