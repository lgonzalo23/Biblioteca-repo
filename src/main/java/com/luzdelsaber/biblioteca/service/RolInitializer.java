package com.luzdelsaber.biblioteca.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.luzdelsaber.biblioteca.model.Rol;
import com.luzdelsaber.biblioteca.repository.RolRepository;

@Component
public class RolInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;

    public RolInitializer(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste("ADMINISTRADOR");
        crearSiNoExiste("ENCARGADO");
        crearSiNoExiste("PRESTATARIO");
    }

    private void crearSiNoExiste(String nombreRol) {
        if (rolRepository.findByNombreRolIgnoreCase(nombreRol).isEmpty()) {
            rolRepository.save(new Rol(nombreRol));
        }
    }
}
