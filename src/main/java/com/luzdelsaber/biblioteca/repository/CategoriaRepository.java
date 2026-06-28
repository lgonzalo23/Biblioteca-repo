package com.luzdelsaber.biblioteca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    List<Categoria> findAllByOrderByNombreAsc();

    @Query(value = """
            SELECT *
            FROM categoria
            WHERE lower(nombre_categoria) LIKE lower(concat('%', :termino, '%'))
               OR lower(descripcion_categoria) LIKE lower(concat('%', :termino, '%'))
               OR lower(estado_categoria) LIKE lower(concat('%', :termino, '%'))
            ORDER BY nombre_categoria ASC
            """, nativeQuery = true)
    List<Categoria> buscarEnUnaTabla(@Param("termino") String termino);
}
