package com.luzdelsaber.biblioteca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.luzdelsaber.biblioteca.model.Autor;

public interface AutorRepository extends JpaRepository<Autor, Integer> {

    List<Autor> findAllByOrderByApellidoAscNombreAsc();
}
