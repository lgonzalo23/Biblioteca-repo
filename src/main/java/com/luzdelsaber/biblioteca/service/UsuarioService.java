package com.luzdelsaber.biblioteca.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luzdelsaber.biblioteca.dto.UsuarioForm;
import com.luzdelsaber.biblioteca.dto.PerfilForm;
import com.luzdelsaber.biblioteca.exception.UsuarioValidationException;
import com.luzdelsaber.biblioteca.model.Rol;
import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.repository.RolRepository;
import com.luzdelsaber.biblioteca.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*@)[A-Za-z\\d@]{8,50}$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    public List<Usuario> listar(String termino) {
        if (StringUtils.hasText(termino)) {
            return usuarioRepository.buscar(termino.trim());
        }
        return usuarioRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getIdUsuario().compareTo(a.getIdUsuario()))
                .toList();
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(Integer idUsuario) {
        return usuarioRepository.findById(idUsuario);
    }

    public Optional<Usuario> autenticar(String correo, String contrasena) {
        if (!StringUtils.hasText(correo) || !StringUtils.hasText(contrasena)) {
            return Optional.empty();
        }
        return usuarioRepository.findByCorreoIgnoreCase(correo.trim())
                .filter(usuario -> usuario.iniciarSesion(correo.trim(), contrasena));
    }

    @Transactional
    public Usuario registrarPublico(UsuarioForm form) {
        Rol prestatario = rolRepository.findByNombreRolIgnoreCase("PRESTATARIO")
                .orElseThrow(() -> new UsuarioValidationException(List.of("No existe el rol PRESTATARIO.")));
        form.setRolId(prestatario.getIdRol());
        form.setEstado(Usuario.ESTADO_ACTIVO);
        return crear(form);
    }

    @Transactional
    public Usuario crear(UsuarioForm form) {
        normalizar(form);
        validarFormulario(form, true, null);

        Usuario usuario = new Usuario();
        aplicarDatos(usuario, form, true);
        usuarioRepository.insertarUsuario(
                usuario.getRol().getIdRol(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getDni(),
                usuario.getCorreo(),
                usuario.getContrasena(),
                usuario.getEstado());
        return usuarioRepository.findByCorreoIgnoreCase(usuario.getCorreo())
                .orElseThrow(() -> new UsuarioValidationException(List.of("No se pudo recuperar el usuario registrado.")));
    }

    @Transactional
    public Usuario actualizar(Integer idUsuario, UsuarioForm form) {
        normalizar(form);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioValidationException(List.of("El usuario seleccionado no existe.")));
        form.setDni(usuario.getDni());
        validarFormulario(form, false, idUsuario);
        validarRol(form.getRolId());

        usuarioRepository.actualizarDatosUsuario(
                idUsuario,
                form.getRolId(),
                form.getNombre(),
                form.getApellido(),
                form.getCorreo(),
                form.getEstado());

        if (StringUtils.hasText(form.getContrasena())) {
            usuarioRepository.actualizarContrasena(idUsuario, form.getContrasena());
        }

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioValidationException(List.of("No se pudo recuperar el usuario actualizado.")));
    }

    @Transactional
    public Usuario actualizarPerfil(Integer idUsuario, PerfilForm form) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioValidationException(List.of("El usuario de la sesion no existe.")));

        UsuarioForm usuarioForm = new UsuarioForm();
        usuarioForm.setRolId(usuario.getRol().getIdRol());
        usuarioForm.setNombre(form.getNombre());
        usuarioForm.setApellido(form.getApellido());
        usuarioForm.setDni(usuario.getDni());
        usuarioForm.setCorreo(form.getCorreo());
        usuarioForm.setEstado(usuario.getEstado());
        usuarioForm.setContrasena(form.getContrasena());
        usuarioForm.setConfirmContrasena(form.getConfirmContrasena());

        normalizar(usuarioForm);
        validarFormulario(usuarioForm, false, idUsuario);
        usuarioRepository.actualizarDatosUsuario(
                idUsuario,
                usuarioForm.getRolId(),
                usuarioForm.getNombre(),
                usuarioForm.getApellido(),
                usuarioForm.getCorreo(),
                usuarioForm.getEstado());

        if (StringUtils.hasText(usuarioForm.getContrasena())) {
            usuarioRepository.actualizarContrasena(idUsuario, usuarioForm.getContrasena());
        }

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioValidationException(List.of("No se pudo recuperar el perfil actualizado.")));
    }

    @Transactional
    public void eliminarFisico(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new UsuarioValidationException(List.of("El usuario seleccionado no existe."));
        }
        usuarioRepository.eliminarFisico(idUsuario);
    }

    @Transactional
    public void eliminarLogico(Integer idUsuario) {
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new UsuarioValidationException(List.of("El usuario seleccionado no existe."));
        }
        usuarioRepository.actualizarEstado(idUsuario, Usuario.ESTADO_INACTIVO);
    }

    private void aplicarDatos(Usuario usuario, UsuarioForm form, boolean actualizarContrasena) {
        Rol rol = validarRol(form.getRolId());
        usuario.setRol(rol);
        usuario.setNombre(form.getNombre());
        usuario.setApellido(form.getApellido());
        usuario.setDni(form.getDni());
        usuario.setCorreo(form.getCorreo());
        usuario.setEstado(form.getEstado());
        if (actualizarContrasena) {
            usuario.setContrasena(form.getContrasena());
        }
    }

    private Rol validarRol(Integer idRol) {
        return rolRepository.findById(idRol)
                .orElseThrow(() -> new UsuarioValidationException(List.of("El rol seleccionado no existe.")));
    }

    private void validarFormulario(UsuarioForm form, boolean requiereContrasena, Integer idActual) {
        List<String> errores = new ArrayList<>();

        validarTexto(form.getNombre(), "nombre", errores);
        validarTexto(form.getApellido(), "apellido", errores);

        if (!StringUtils.hasText(form.getDni()) || !form.getDni().matches("\\d{8}")) {
            errores.add("El DNI debe tener exactamente 8 digitos numericos.");
        }

        if (!StringUtils.hasText(form.getCorreo()) || form.getCorreo().length() > 50) {
            errores.add("El correo electronico es obligatorio y no puede superar 50 caracteres.");
        }

        if (!Usuario.ESTADO_ACTIVO.equals(form.getEstado()) && !Usuario.ESTADO_INACTIVO.equals(form.getEstado())) {
            errores.add("El estado debe ser ACTIVO o INACTIVO.");
        }

        validarUnicos(form, idActual, errores);
        validarContrasena(form, requiereContrasena, errores);

        if (!errores.isEmpty()) {
            throw new UsuarioValidationException(errores);
        }
    }

    private void validarTexto(String valor, String campo, List<String> errores) {
        if (!StringUtils.hasText(valor)) {
            errores.add("El " + campo + " es obligatorio.");
            return;
        }
        if (valor.length() < 2 || valor.length() > 50) {
            errores.add("El " + campo + " debe tener entre 2 y 50 caracteres.");
        }
    }

    private void validarUnicos(UsuarioForm form, Integer idActual, List<String> errores) {
        usuarioRepository.findByCorreoIgnoreCase(form.getCorreo()).ifPresent(existente -> {
            if (!existente.getIdUsuario().equals(idActual)) {
                errores.add("El correo electronico ya esta registrado.");
            }
        });

        usuarioRepository.findByDni(form.getDni()).ifPresent(existente -> {
            if (!existente.getIdUsuario().equals(idActual)) {
                errores.add("El DNI ya esta registrado.");
            }
        });
    }

    private void validarContrasena(UsuarioForm form, boolean requiereContrasena, List<String> errores) {
        boolean tieneContrasena = StringUtils.hasText(form.getContrasena());
        if (requiereContrasena && !tieneContrasena) {
            errores.add("La contrasena es obligatoria.");
            return;
        }
        if (tieneContrasena && !PASSWORD_PATTERN.matcher(form.getContrasena()).matches()) {
            errores.add("La contrasena debe tener minimo 8 caracteres, mayuscula, minuscula, numero y solo @ como caracter especial.");
        }
        if (tieneContrasena && !form.getContrasena().equals(form.getConfirmContrasena())) {
            errores.add("La confirmacion de contrasena no coincide.");
        }
    }

    private void normalizar(UsuarioForm form) {
        form.setNombre(limpiar(form.getNombre()));
        form.setApellido(limpiar(form.getApellido()));
        form.setDni(limpiar(form.getDni()));
        form.setCorreo(limpiar(form.getCorreo()));
        form.setEstado(StringUtils.hasText(form.getEstado()) ? form.getEstado().trim().toUpperCase() : Usuario.ESTADO_ACTIVO);
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
