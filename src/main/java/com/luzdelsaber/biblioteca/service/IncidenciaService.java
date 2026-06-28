package com.luzdelsaber.biblioteca.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luzdelsaber.biblioteca.exception.ReservaValidationException;
import com.luzdelsaber.biblioteca.model.Incidencia;
import com.luzdelsaber.biblioteca.model.Sancion;
import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.repository.IncidenciaRepository;
import com.luzdelsaber.biblioteca.repository.SancionRepository;
import com.luzdelsaber.biblioteca.repository.UsuarioRepository;

@Service
public class IncidenciaService {

    private static final int RETRASOS_PARA_SANCION = 3;
    private static final int DIAS_SANCION_RETRASO = 15;

    private final IncidenciaRepository incidenciaRepository;
    private final SancionRepository sancionRepository;
    private final UsuarioRepository usuarioRepository;

    public IncidenciaService(
            IncidenciaRepository incidenciaRepository,
            SancionRepository sancionRepository,
            UsuarioRepository usuarioRepository) {
        this.incidenciaRepository = incidenciaRepository;
        this.sancionRepository = sancionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Incidencia> listarIncidencias(String termino) {
        List<Incidencia> incidencias = StringUtils.hasText(termino)
                ? incidenciaRepository.buscarPorUsuarioOTipo(termino.trim())
                : incidenciaRepository.findAllByOrderByFechaIncidenciaDescIdIncidenciaDesc();
        incidencias.forEach(this::agregarDatosSancion);
        return incidencias;
    }

    private void agregarDatosSancion(Incidencia incidencia) {
        incidencia.setSancion(
                sancionRepository.findFirstByIncidenciaIdIncidenciaOrderByIdSancionDesc(incidencia.getIdIncidencia())
                        .orElse(null));
        if (!"RETRASO".equals(incidencia.getTipo())) {
            incidencia.setSancionable(true);
            return;
        }

        Usuario usuario = incidencia.getPrestamo() == null ? null : incidencia.getPrestamo().getUsuario();
        long retrasos = usuario == null ? 0 : incidenciaRepository.contarRetrasosDelMesPorUsuario(usuario.getIdUsuario());
        incidencia.setSancionable(retrasos >= RETRASOS_PARA_SANCION);
    }

    @Transactional
    public void aplicarSancion(Integer idIncidencia, String tipoSancion, Integer diasSuspension) {
        if (!StringUtils.hasText(tipoSancion)) {
            throw new ReservaValidationException(List.of("Debe indicar el tipo de sancion."));
        }
        String tipoNormalizado = tipoSancion.trim().toUpperCase();
        if (!Sancion.TIPO_SUSPENSION_TEMPORAL.equals(tipoNormalizado) && !Sancion.TIPO_SUSPENSION_TOTAL.equals(tipoNormalizado)) {
            throw new ReservaValidationException(List.of("El tipo de sancion seleccionado no es valido."));
        }
        if (Sancion.TIPO_SUSPENSION_TEMPORAL.equals(tipoNormalizado) && (diasSuspension == null || diasSuspension < 1)) {
            throw new ReservaValidationException(List.of("Debe indicar al menos 1 dia para la suspension temporal."));
        }
        Incidencia incidencia = incidenciaRepository.findById(idIncidencia)
                .orElseThrow(() -> new ReservaValidationException(List.of("La incidencia seleccionada no existe.")));
        if (sancionRepository.existsByIncidenciaIdIncidencia(idIncidencia)) {
            throw new ReservaValidationException(List.of("Esta incidencia ya tiene una sancion registrada."));
        }

        Usuario usuario = incidencia.getPrestamo() == null ? null : incidencia.getPrestamo().getUsuario();
        if ("RETRASO".equals(incidencia.getTipo())) {
            long retrasos = usuario == null ? 0 : incidenciaRepository.contarRetrasosDelMesPorUsuario(usuario.getIdUsuario());
            if (retrasos < RETRASOS_PARA_SANCION) {
                throw new ReservaValidationException(List.of(
                        "Las entregas tardias solo se sancionan cuando el usuario acumula 3 retrasos."));
            }
            tipoNormalizado = Sancion.TIPO_SUSPENSION_TEMPORAL;
            diasSuspension = DIAS_SANCION_RETRASO;
        }

        Sancion sancion = new Sancion();
        sancion.setIncidencia(incidencia);
        sancion.setTipo(tipoNormalizado);
        sancion.setDiasSuspension(Sancion.TIPO_SUSPENSION_TEMPORAL.equals(tipoNormalizado) ? diasSuspension : null);
        sancion.setEstado(Sancion.ESTADO_ACTIVA);
        sancionRepository.save(sancion);

        if (usuario != null) {
            usuarioRepository.actualizarEstado(usuario.getIdUsuario(), Usuario.ESTADO_INACTIVO);
        }
    }
}
