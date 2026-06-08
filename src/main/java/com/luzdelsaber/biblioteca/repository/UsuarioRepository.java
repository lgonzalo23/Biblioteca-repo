package com.luzdelsaber.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByDni(String dni);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    @Query("""
            select u from Usuario u
            where lower(u.nombre) like lower(concat('%', :termino, '%'))
               or lower(u.apellido) like lower(concat('%', :termino, '%'))
               or u.dni like concat('%', :termino, '%')
               or lower(u.correo) like lower(concat('%', :termino, '%'))
            order by u.idUsuario desc
            """)
    List<Usuario> buscar(@Param("termino") String termino);
}
