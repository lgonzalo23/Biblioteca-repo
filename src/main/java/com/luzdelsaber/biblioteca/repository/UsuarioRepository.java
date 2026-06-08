package com.luzdelsaber.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.luzdelsaber.biblioteca.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    Optional<Usuario> findByDni(String dni);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByDni(String dni);

    @Modifying
    @Query(value = """
            INSERT INTO usuario
                (id_rol, nombre_usuario, apellido_usuario, dni_usuario, email_usuario, contrase\u00f1a_usuario, estado_usuario)
            VALUES
                (:rolId, :nombre, :apellido, :dni, :correo, :contrasena, :estado)
            """, nativeQuery = true)
    void insertarUsuario(
            @Param("rolId") Integer rolId,
            @Param("nombre") String nombre,
            @Param("apellido") String apellido,
            @Param("dni") String dni,
            @Param("correo") String correo,
            @Param("contrasena") String contrasena,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET id_rol = :rolId,
                nombre_usuario = :nombre,
                apellido_usuario = :apellido,
                email_usuario = :correo,
                estado_usuario = :estado
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    void actualizarDatosUsuario(
            @Param("idUsuario") Integer idUsuario,
            @Param("rolId") Integer rolId,
            @Param("nombre") String nombre,
            @Param("apellido") String apellido,
            @Param("correo") String correo,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET contrase\u00f1a_usuario = :contrasena
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    void actualizarContrasena(
            @Param("idUsuario") Integer idUsuario,
            @Param("contrasena") String contrasena);

    @Modifying
    @Query(value = """
            UPDATE usuario
            SET estado_usuario = :estado
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    void actualizarEstado(
            @Param("idUsuario") Integer idUsuario,
            @Param("estado") String estado);

    @Modifying
    @Query(value = """
            DELETE FROM usuario
            WHERE id_usuario = :idUsuario
            """, nativeQuery = true)
    void eliminarFisico(@Param("idUsuario") Integer idUsuario);

    @Query(value = """
            SELECT u.*
            FROM usuario u
            INNER JOIN rol r ON r.id_rol = u.id_rol
            WHERE lower(u.nombre_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.apellido_usuario) LIKE lower(concat('%', :termino, '%'))
               OR u.dni_usuario LIKE concat('%', :termino, '%')
               OR lower(u.email_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(u.estado_usuario) LIKE lower(concat('%', :termino, '%'))
               OR lower(r.nombre_rol) LIKE lower(concat('%', :termino, '%'))
            ORDER BY u.id_usuario DESC
            """, nativeQuery = true)
    List<Usuario> buscar(@Param("termino") String termino);
}
