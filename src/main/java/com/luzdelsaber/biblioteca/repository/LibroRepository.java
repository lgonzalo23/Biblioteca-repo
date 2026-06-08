package com.luzdelsaber.biblioteca.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Libro;

public interface LibroRepository extends JpaRepository<Libro, Integer> {

    Optional<Libro> findByIsbnIgnoreCase(String isbn);

    @Modifying
    @Query(value = """
            INSERT INTO libro
                (id_categoria, titulo_libro, isbn, fecha_publicacion, estado_libro, stock, ubicacion, url_imagen)
            VALUES
                (:categoriaId, :titulo, :isbn, :fechaPublicacion, :estado, :stock, :ubicacion, :urlImagen)
            """, nativeQuery = true)
    void insertarLibro(
            @Param("categoriaId") Integer categoriaId,
            @Param("titulo") String titulo,
            @Param("isbn") String isbn,
            @Param("fechaPublicacion") LocalDate fechaPublicacion,
            @Param("estado") String estado,
            @Param("stock") Integer stock,
            @Param("ubicacion") String ubicacion,
            @Param("urlImagen") String urlImagen);

    @Modifying
    @Query(value = """
            UPDATE libro
            SET id_categoria = :categoriaId,
                titulo_libro = :titulo,
                fecha_publicacion = :fechaPublicacion,
                estado_libro = :estado,
                stock = :stock,
                ubicacion = :ubicacion,
                url_imagen = :urlImagen
            WHERE id_libro = :idLibro
            """, nativeQuery = true)
    void actualizarLibro(
            @Param("idLibro") Integer idLibro,
            @Param("categoriaId") Integer categoriaId,
            @Param("titulo") String titulo,
            @Param("fechaPublicacion") LocalDate fechaPublicacion,
            @Param("estado") String estado,
            @Param("stock") Integer stock,
            @Param("ubicacion") String ubicacion,
            @Param("urlImagen") String urlImagen);

    @Modifying
    @Query(value = """
            UPDATE libro
            SET estado_libro = :estado
            WHERE id_libro = :idLibro
            """, nativeQuery = true)
    void actualizarEstado(
            @Param("idLibro") Integer idLibro,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            DELETE FROM libro_autor
            WHERE id_libro = :idLibro
            """, nativeQuery = true)
    void eliminarAutoresDelLibro(@Param("idLibro") Integer idLibro);

    @Modifying
    @Query(value = """
            INSERT INTO libro_autor
                (id_libro, id_autor)
            VALUES
                (:idLibro, :idAutor)
            """, nativeQuery = true)
    void insertarAutorDelLibro(
            @Param("idLibro") Integer idLibro,
            @Param("idAutor") Integer idAutor);

    @Modifying
    @Query(value = """
            DELETE FROM libro
            WHERE id_libro = :idLibro
            """, nativeQuery = true)
    void eliminarFisico(@Param("idLibro") Integer idLibro);

    @Query(value = """
            SELECT *
            FROM libro
            ORDER BY id_libro DESC
            """, nativeQuery = true)
    List<Libro> listarOrdenado();

    @Query(value = """
            SELECT DISTINCT l.*
            FROM libro l
            LEFT JOIN categoria c ON c.id_categoria = l.id_categoria
            LEFT JOIN libro_autor la ON la.id_libro = l.id_libro
            LEFT JOIN autor a ON a.id_autor = la.id_autor
            WHERE lower(l.titulo_libro) LIKE lower(concat('%', :termino, '%'))
               OR lower(l.isbn) LIKE lower(concat('%', :termino, '%'))
               OR lower(l.ubicacion) LIKE lower(concat('%', :termino, '%'))
               OR lower(coalesce(l.url_imagen, '')) LIKE lower(concat('%', :termino, '%'))
               OR lower(l.estado_libro) LIKE lower(concat('%', :termino, '%'))
               OR lower(c.nombre_categoria) LIKE lower(concat('%', :termino, '%'))
               OR lower(a.nombre_autor) LIKE lower(concat('%', :termino, '%'))
               OR lower(a.apellido_autor) LIKE lower(concat('%', :termino, '%'))
            ORDER BY l.id_libro DESC
            """, nativeQuery = true)
    List<Libro> buscar(@Param("termino") String termino);

    @Query(value = """
            SELECT l.*
            FROM libro l
            INNER JOIN categoria c ON c.id_categoria = l.id_categoria
            WHERE l.estado_libro = 'DISPONIBLE'
              AND l.stock > 0
              AND c.estado_categoria = 'ACTIVO'
              AND (:categoriaId IS NULL OR l.id_categoria = :categoriaId)
            ORDER BY l.titulo_libro ASC
            """, nativeQuery = true)
    List<Libro> listarCatalogo(@Param("categoriaId") Integer categoriaId);

    @Query(value = """
            SELECT DISTINCT l.*
            FROM libro l
            LEFT JOIN categoria c ON c.id_categoria = l.id_categoria
            LEFT JOIN libro_autor la ON la.id_libro = l.id_libro
            LEFT JOIN autor a ON a.id_autor = la.id_autor
            WHERE l.estado_libro = 'DISPONIBLE'
              AND l.stock > 0
              AND c.estado_categoria = 'ACTIVO'
              AND (:categoriaId IS NULL OR l.id_categoria = :categoriaId)
              AND (
                    lower(l.titulo_libro) LIKE lower(concat('%', :termino, '%'))
                 OR lower(l.isbn) LIKE lower(concat('%', :termino, '%'))
                 OR lower(l.ubicacion) LIKE lower(concat('%', :termino, '%'))
                 OR lower(coalesce(l.url_imagen, '')) LIKE lower(concat('%', :termino, '%'))
                 OR lower(c.nombre_categoria) LIKE lower(concat('%', :termino, '%'))
                 OR lower(a.nombre_autor) LIKE lower(concat('%', :termino, '%'))
                 OR lower(a.apellido_autor) LIKE lower(concat('%', :termino, '%'))
              )
            ORDER BY l.titulo_libro ASC
            """, nativeQuery = true)
    List<Libro> buscarCatalogo(
            @Param("termino") String termino,
            @Param("categoriaId") Integer categoriaId);
}
