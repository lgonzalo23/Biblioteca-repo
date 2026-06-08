package com.luzdelsaber.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Libro;

public interface LibroRepository extends JpaRepository<Libro, Integer> {

    Optional<Libro> findByIsbnIgnoreCase(String isbn);

    @Query("""
            select distinct l from Libro l
            left join l.autores a
            where lower(l.titulo) like lower(concat('%', :termino, '%'))
               or lower(l.isbn) like lower(concat('%', :termino, '%'))
               or lower(l.ubicacion) like lower(concat('%', :termino, '%'))
               or lower(l.urlImagen) like lower(concat('%', :termino, '%'))
               or lower(l.estado) like lower(concat('%', :termino, '%'))
               or lower(l.categoria.nombre) like lower(concat('%', :termino, '%'))
               or lower(a.nombre) like lower(concat('%', :termino, '%'))
               or lower(a.apellido) like lower(concat('%', :termino, '%'))
            order by l.idLibro desc
            """)
    List<Libro> buscar(@Param("termino") String termino);
}
