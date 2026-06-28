package com.luzdelsaber.biblioteca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Autor;

public interface AutorRepository extends JpaRepository<Autor, Integer> {

    List<Autor> findAllByOrderByApellidoAscNombreAsc();

    @Query(value = """
            SELECT *
            FROM autor
            WHERE lower(nombre_autor) LIKE lower(concat('%', :termino, '%'))
               OR lower(apellido_autor) LIKE lower(concat('%', :termino, '%'))
               OR lower(nacionalidad_autor) LIKE lower(concat('%', :termino, '%'))
               OR lower(estado_autor) LIKE lower(concat('%', :termino, '%'))
            ORDER BY apellido_autor ASC, nombre_autor ASC
            """, nativeQuery = true)
    List<Autor> buscarEnUnaTabla(@Param("termino") String termino);
}
