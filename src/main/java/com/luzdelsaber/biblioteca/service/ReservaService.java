package com.luzdelsaber.biblioteca.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luzdelsaber.biblioteca.exception.ReservaValidationException;
import com.luzdelsaber.biblioteca.model.Incidencia;
import com.luzdelsaber.biblioteca.model.Libro;
import com.luzdelsaber.biblioteca.model.Prestamo;
import com.luzdelsaber.biblioteca.model.Reserva;
import com.luzdelsaber.biblioteca.model.ReservaDetalle;
import com.luzdelsaber.biblioteca.model.Sancion;
import com.luzdelsaber.biblioteca.model.Usuario;
import com.luzdelsaber.biblioteca.repository.IncidenciaRepository;
import com.luzdelsaber.biblioteca.repository.LibroRepository;
import com.luzdelsaber.biblioteca.repository.PrestamoRepository;
import com.luzdelsaber.biblioteca.repository.ReservaDetalleRepository;
import com.luzdelsaber.biblioteca.repository.ReservaRepository;
import com.luzdelsaber.biblioteca.repository.SancionRepository;
import com.luzdelsaber.biblioteca.repository.UsuarioRepository;

@Service
public class ReservaService {

    private static final int HORAS_PRESTAMO_MINIMO = 1;
    private static final int HORAS_PRESTAMO_MAXIMO = 3;
    private static final int LIBROS_POR_RESERVA_MAXIMO = 3;
    private static final int RETRASOS_PARA_SANCION = 3;
    private static final int DIAS_SANCION_RETRASO = 15;
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 0);
    private static final LocalTime HORA_CIERRE = LocalTime.of(19, 0);

    private final ReservaRepository reservaRepository;
    private final ReservaDetalleRepository reservaDetalleRepository;
    private final PrestamoRepository prestamoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;
    private final SancionRepository sancionRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            ReservaDetalleRepository reservaDetalleRepository,
            PrestamoRepository prestamoRepository,
            IncidenciaRepository incidenciaRepository,
            LibroRepository libroRepository,
            UsuarioRepository usuarioRepository,
            SancionRepository sancionRepository) {
        this.reservaRepository = reservaRepository;
        this.reservaDetalleRepository = reservaDetalleRepository;
        this.prestamoRepository = prestamoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.libroRepository = libroRepository;
        this.usuarioRepository = usuarioRepository;
        this.sancionRepository = sancionRepository;
    }

    @Transactional
    public List<Reserva> listarReservas(Integer idUsuario, String termino) {
        List<Reserva> reservas;
        if (StringUtils.hasText(termino)) {
            reservas = reservaRepository.buscarPorUsuario(idUsuario, termino.trim());
        } else {
            reservas = reservaRepository.listarPorUsuario(idUsuario);
        }
        reservas.forEach(this::vencerReservaSiCorresponde);
        return reservas;
    }

    @Transactional
    public List<Reserva> listarPedidos(String termino) {
        List<Reserva> reservas;
        if (StringUtils.hasText(termino)) {
            reservas = reservaRepository.buscarTodas(termino.trim());
        } else {
            reservas = reservaRepository.listarTodas();
        }
        reservas.forEach(this::vencerReservaSiCorresponde);
        reservas.forEach(this::agregarDatosPrestamo);
        return reservas;
    }

    @Transactional
    public Reserva crearReserva(Integer idUsuario, List<Integer> idsLibro, LocalDate fechaReserva, LocalTime horaReserva,
            Integer horasPrestamo) {
        List<Integer> idsLibroUnicos = validarDatosBase(idUsuario, idsLibro, fechaReserva, horaReserva, horasPrestamo);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ReservaValidationException(List.of("El usuario de la sesion no existe.")));
        if (!Usuario.ESTADO_ACTIVO.equals(usuario.getEstado())) {
            throw new ReservaValidationException(List.of("El usuario debe estar ACTIVO para reservar libros."));
        }

        List<Libro> libros = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        for (Integer idLibro : idsLibroUnicos) {
            Libro libro = libroRepository.findById(idLibro)
                    .orElse(null);
            if (libro == null) {
                errores.add("Uno de los libros seleccionados no existe.");
                continue;
            }
            if (!libro.verificarDisponibilidad()) {
                errores.add("El libro \"" + libro.getTitulo() + "\" no esta disponible para reserva.");
            }

            Integer reservasActivas = reservaRepository.contarActivasPorUsuarioYLibro(idUsuario, idLibro);
            if (reservasActivas != null && reservasActivas > 0) {
                errores.add("Ya tienes una reserva activa para el libro \"" + libro.getTitulo() + "\".");
            }
            libros.add(libro);
        }
        if (!errores.isEmpty()) {
            throw new ReservaValidationException(errores);
        }

        Reserva reserva = new Reserva(usuario, fechaReserva);
        Reserva reservaRegistrada = reservaRepository.save(reserva);
        LocalDateTime fechaHoraLimite = calcularFechaHoraLimite(fechaReserva, horaReserva, horasPrestamo);

        List<ReservaDetalle> detalles = new ArrayList<>();
        for (Libro libro : libros) {
            int librosActualizados = libroRepository.reservarEjemplar(libro.getIdLibro());
            if (librosActualizados == 0) {
                throw new ReservaValidationException(
                        List.of("No se pudo reservar \"" + libro.getTitulo() + "\" porque ya no tiene stock disponible."));
            }

            ReservaDetalle detalle = new ReservaDetalle();
            detalle.setReserva(reservaRegistrada);
            detalle.setLibro(libro);
            detalle.setFechaRecojoLimite(fechaHoraLimite.toLocalDate());
            detalle.setHoraReserva(horaReserva);
            detalle.setHorasPrestamo(horasPrestamo);
            detalle.setHoraRecojoLimite(fechaHoraLimite.toLocalTime());
            detalle.setEstadoDetalleReserva(ReservaDetalle.ESTADO_ACTIVA);
            detalles.add(reservaDetalleRepository.save(detalle));
        }
        reservaRegistrada.setDetalles(detalles);

        return reservaRegistrada;
    }

    @Transactional
    public void cancelarReserva(Integer idUsuario, Integer idReserva) {
        Reserva reserva = reservaRepository.buscarDelUsuario(idReserva, idUsuario)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        cancelarReservaActiva(reserva);
    }

    @Transactional
    public void cancelarPedidoReserva(Integer idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        cancelarReservaActiva(reserva);
    }

    @Transactional
    public void eliminarPedidoReservaFisico(Integer idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!prestamoRepository.findByReservaIdReservaOrderByIdPrestamoAsc(idReserva).isEmpty()) {
            throw new ReservaValidationException(
                    List.of("No se puede eliminar definitivamente una reserva que ya tiene prestamos registrados."));
        }
        if (reserva.estaActiva()) {
            liberarEjemplaresReservados(reserva);
        }

        reservaDetalleRepository.eliminarPorReserva(idReserva);
        reservaRepository.eliminarFisico(idReserva);
    }

    @Transactional
    public void actualizarReserva(Integer idUsuario, Integer idReserva, LocalDate fechaReserva, LocalTime horaReserva,
            Integer horasPrestamo) {
        if (idUsuario == null) {
            throw new ReservaValidationException(List.of("Debes iniciar sesion para modificar una reserva."));
        }
        validarFechaHoraYDuracion(fechaReserva, horaReserva, horasPrestamo);

        Reserva reserva = reservaRepository.buscarDelUsuario(idReserva, idUsuario)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!reserva.estaActiva()) {
            throw new ReservaValidationException(List.of("Solo se pueden modificar reservas activas."));
        }

        int reservasActualizadas = reservaRepository.actualizarFechaReserva(
                idReserva,
                idUsuario,
                fechaReserva);
        if (reservasActualizadas == 0) {
            throw new ReservaValidationException(List.of("No se pudo actualizar la reserva."));
        }
        LocalDateTime fechaHoraLimite = calcularFechaHoraLimite(fechaReserva, horaReserva, horasPrestamo);
        reservaDetalleRepository.actualizarFechaRecojoPorReserva(
                idReserva,
                fechaHoraLimite.toLocalDate(),
                horaReserva,
                horasPrestamo,
                fechaHoraLimite.toLocalTime());
    }

    @Transactional
    public void convertirReservaEnPrestamo(Integer idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!reserva.estaActiva()) {
            throw new ReservaValidationException(List.of("Solo se pueden convertir reservas activas en prestamo."));
        }
        if (!estaDisponibleParaPrestar(reserva)) {
            throw new ReservaValidationException(List.of("El prestamo solo puede registrarse desde la fecha y hora de la reserva."));
        }
        if (reserva.getDetalles() == null || reserva.getDetalles().isEmpty()) {
            throw new ReservaValidationException(List.of("La reserva no tiene libros para convertir en prestamo."));
        }

        int prestamosCreados = 0;
        for (ReservaDetalle detalle : reserva.getDetalles()) {
            if (!ReservaDetalle.ESTADO_ACTIVA.equals(detalle.getEstadoDetalleReserva())) {
                continue;
            }
            if (detalle.getLibro() == null) {
                throw new ReservaValidationException(List.of("Uno de los detalles de la reserva no tiene libro asociado."));
            }

            Prestamo prestamo = new Prestamo();
            prestamo.setUsuario(reserva.getUsuario());
            prestamo.setLibro(detalle.getLibro());
            prestamo.setReserva(reserva);
            prestamo.setFechaPrestamo(reserva.getFechaReserva());
            prestamo.setHoraInicio(detalle.getHoraReserva());
            prestamo.setHoraFin(detalle.getHoraRecojoLimite());
            prestamo.setEstado(Prestamo.ESTADO_ACTIVO);
            prestamoRepository.save(prestamo);
            prestamosCreados++;
        }

        if (prestamosCreados == 0) {
            throw new ReservaValidationException(List.of("La reserva no tiene detalles activos para convertir en prestamo."));
        }

        reservaRepository.actualizarEstado(idReserva, Reserva.ESTADO_PRESTADA);
        reservaDetalleRepository.actualizarEstadoPorReserva(idReserva, ReservaDetalle.ESTADO_PRESTADA);
    }

    @Transactional
    public void postergarReservaDesdePedidos(Integer idReserva, LocalDate fechaReserva, LocalTime horaReserva,
            Integer horasPrestamo) {
        validarFechaHoraYDuracion(fechaReserva, horaReserva, horasPrestamo);

        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!reserva.estaActiva()) {
            throw new ReservaValidationException(List.of("Solo se pueden postergar reservas activas."));
        }

        int reservasActualizadas = reservaRepository.actualizarFechaReserva(
                idReserva,
                reserva.getUsuario().getIdUsuario(),
                fechaReserva);
        if (reservasActualizadas == 0) {
            throw new ReservaValidationException(List.of("No se pudo postergar la reserva."));
        }

        LocalDateTime fechaHoraLimite = calcularFechaHoraLimite(fechaReserva, horaReserva, horasPrestamo);
        reservaDetalleRepository.actualizarFechaRecojoPorReserva(
                idReserva,
                fechaHoraLimite.toLocalDate(),
                horaReserva,
                horasPrestamo,
                fechaHoraLimite.toLocalTime());
    }

    @Transactional
    public void cancelarPrestamo(Integer idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!Reserva.ESTADO_PRESTADA.equals(reserva.getEstado())) {
            throw new ReservaValidationException(List.of("Solo se pueden cancelar prestamos activos."));
        }
        List<Prestamo> prestamos = prestamoRepository.findByReservaIdReservaOrderByIdPrestamoAsc(idReserva);
        if (prestamos.isEmpty()) {
            throw new ReservaValidationException(List.of("Esta reserva no tiene prestamos registrados."));
        }

        prestamoRepository.actualizarEstadoPorReserva(idReserva, Prestamo.ESTADO_CANCELADO);
        reservaRepository.actualizarEstado(idReserva, Reserva.ESTADO_CANCELADA);
        reservaDetalleRepository.actualizarEstadoPorReserva(idReserva, ReservaDetalle.ESTADO_CANCELADA);
        for (ReservaDetalle detalle : reserva.getDetalles()) {
            if (detalle.getLibro() != null) {
                libroRepository.liberarEjemplarReservado(detalle.getLibro().getIdLibro());
            }
        }
    }

    @Transactional
    public void postergarPrestamo(Integer idReserva, Integer horasExtra) {
        ampliarPrestamo(idReserva, horasExtra);
    }

    @Transactional
    public void ampliarPrestamo(Integer idReserva, Integer horasExtra) {
        if (horasExtra == null) {
            throw new ReservaValidationException(List.of("Debe indicar cuantas horas extra tendra el prestamo."));
        }
        if (horasExtra < HORAS_PRESTAMO_MINIMO || horasExtra > HORAS_PRESTAMO_MAXIMO) {
            throw new ReservaValidationException(List.of("La ampliacion puede ser de 1, 2 o 3 horas extra."));
        }
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!Reserva.ESTADO_PRESTADA.equals(reserva.getEstado())) {
            throw new ReservaValidationException(List.of("Solo se pueden ampliar prestamos activos."));
        }

        List<Prestamo> prestamosActivos = prestamoRepository.findByReservaIdReservaAndEstadoOrderByIdPrestamoAsc(
                idReserva,
                Prestamo.ESTADO_ACTIVO);
        if (prestamosActivos.isEmpty()) {
            throw new ReservaValidationException(List.of("No hay prestamos activos para ampliar."));
        }
        Prestamo prestamoPrincipal = prestamosActivos.get(0);
        LocalDateTime nuevaFechaHoraFin = calcularFechaHoraFinPrestamo(prestamoPrincipal).plusHours(horasExtra);
        if (nuevaFechaHoraFin.toLocalDate().isAfter(prestamoPrincipal.getFechaPrestamo())
                || nuevaFechaHoraFin.toLocalTime().isAfter(HORA_CIERRE)) {
            throw new ReservaValidationException(
                    List.of("La nueva hora de devolucion no puede pasar de las 07:00 p.m."));
        }
        LocalTime nuevaHoraFin = nuevaFechaHoraFin.toLocalTime();

        int prestamosActualizados = prestamoRepository.ampliarPorReserva(idReserva, nuevaHoraFin);
        if (prestamosActualizados == 0) {
            throw new ReservaValidationException(List.of("No hay prestamos activos para ampliar."));
        }
        reservaDetalleRepository.actualizarFechaRecojoPorReserva(
                idReserva,
                reserva.getFechaReserva(),
                reserva.getHoraReserva(),
                calcularHorasTotalesPrestamo(prestamoPrincipal, nuevaFechaHoraFin),
                nuevaHoraFin);
    }

    @Transactional
    public void registrarDevolucion(Integer idReserva) {
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaValidationException(List.of("La reserva seleccionada no existe.")));
        if (!Reserva.ESTADO_PRESTADA.equals(reserva.getEstado())) {
            throw new ReservaValidationException(List.of("Solo se puede registrar devolucion de prestamos activos."));
        }
        List<Prestamo> prestamos = prestamoRepository.findByReservaIdReservaAndEstadoOrderByIdPrestamoAsc(
                idReserva,
                Prestamo.ESTADO_ACTIVO);
        if (prestamos.isEmpty()) {
            throw new ReservaValidationException(List.of("No hay prestamos activos para registrar devolucion."));
        }

        LocalDate fechaDevolucionReal = LocalDate.now();
        LocalTime horaDevolucionReal = LocalTime.now().withSecond(0).withNano(0);
        boolean devolucionTardia = esDevolucionTardia(prestamos.get(0), fechaDevolucionReal, horaDevolucionReal);
        String estadoDevolucion = devolucionTardia
                ? Prestamo.ESTADO_DEVUELTO_TARDIO
                : Prestamo.ESTADO_DEVUELTO;

        int prestamosDevueltos = prestamoRepository.registrarDevolucionPorReserva(
                idReserva,
                estadoDevolucion,
                fechaDevolucionReal,
                horaDevolucionReal);
        if (prestamosDevueltos == 0) {
            throw new ReservaValidationException(List.of("No hay prestamos activos para registrar devolucion."));
        }
        if (devolucionTardia) {
            registrarRetrasosAutomaticos(prestamos);
        }
        reservaRepository.actualizarEstado(idReserva, Reserva.ESTADO_DEVUELTA);
        reservaDetalleRepository.actualizarEstadoPorReserva(idReserva, ReservaDetalle.ESTADO_DEVUELTA);
        for (ReservaDetalle detalle : reserva.getDetalles()) {
            if (detalle.getLibro() != null) {
                libroRepository.liberarEjemplarReservado(detalle.getLibro().getIdLibro());
            }
        }
    }

    @Transactional
    public void registrarIncidencia(Integer idPrestamo, String tipo, String descripcion) {
        if (!StringUtils.hasText(tipo) || !StringUtils.hasText(descripcion)) {
            throw new ReservaValidationException(List.of("Debe completar el tipo y la descripcion de la incidencia."));
        }
        Prestamo prestamo = prestamoRepository.findById(idPrestamo)
                .orElseThrow(() -> new ReservaValidationException(List.of("El prestamo seleccionado no existe.")));
        if (!Prestamo.ESTADO_DEVUELTO.equals(prestamo.getEstado())
                && !Prestamo.ESTADO_DEVUELTO_TARDIO.equals(prestamo.getEstado())) {
            throw new ReservaValidationException(List.of("Solo se pueden registrar incidencias despues de registrar la devolucion."));
        }
        String tipoNormalizado = tipo.trim().toUpperCase();
        if ("RETRASO".equals(tipoNormalizado) && existeRetrasoParaPrestamoGeneral(prestamo)) {
            throw new ReservaValidationException(List.of("Este prestamo general ya tiene una incidencia por retraso registrada."));
        }
        if (!"RETRASO".equals(tipoNormalizado) && incidenciaRepository.existsByPrestamoIdPrestamo(idPrestamo)) {
            throw new ReservaValidationException(List.of("Este prestamo ya tiene una incidencia registrada."));
        }

        Incidencia incidencia = new Incidencia();
        incidencia.setPrestamo(prestamo);
        incidencia.setTipo(tipoNormalizado);
        incidencia.setDescripcion(descripcion.trim());
        incidencia.setFechaIncidencia(LocalDate.now());
        Incidencia incidenciaRegistrada = incidenciaRepository.save(incidencia);
        if ("RETRASO".equals(tipoNormalizado)) {
            aplicarSancionPorRetrasosSiCorresponde(incidenciaRegistrada);
        }
    }

    private void registrarRetrasosAutomaticos(List<Prestamo> prestamos) {
        if (prestamos == null || prestamos.isEmpty()) {
            return;
        }
        Prestamo prestamoPrincipal = prestamos.get(0);
        if (existeRetrasoParaPrestamoGeneral(prestamoPrincipal)) {
            return;
        }
        Incidencia incidencia = new Incidencia();
        incidencia.setPrestamo(prestamoPrincipal);
        incidencia.setTipo("RETRASO");
        incidencia.setDescripcion("Devolucion tardia del prestamo general asociado a la reserva.");
        incidencia.setFechaIncidencia(LocalDate.now());
        Incidencia incidenciaRegistrada = incidenciaRepository.save(incidencia);
        aplicarSancionPorRetrasosSiCorresponde(incidenciaRegistrada);
    }

    private void aplicarSancionPorRetrasosSiCorresponde(Incidencia incidencia) {
        Usuario usuario = incidencia.getPrestamo() == null ? null : incidencia.getPrestamo().getUsuario();
        if (usuario == null) {
            return;
        }
        long retrasos = incidenciaRepository.contarRetrasosDelMesPorUsuario(usuario.getIdUsuario());
        if (retrasos < RETRASOS_PARA_SANCION || !sancionRepository.buscarActivasPorUsuario(usuario.getIdUsuario()).isEmpty()) {
            return;
        }

        Sancion sancion = new Sancion();
        sancion.setIncidencia(incidencia);
        sancion.setTipo(Sancion.TIPO_SUSPENSION_TEMPORAL);
        sancion.setDiasSuspension(DIAS_SANCION_RETRASO);
        sancion.setEstado(Sancion.ESTADO_ACTIVA);
        sancionRepository.save(sancion);
        usuarioRepository.actualizarEstado(usuario.getIdUsuario(), Usuario.ESTADO_INACTIVO);
    }

    private boolean existeRetrasoParaPrestamoGeneral(Prestamo prestamo) {
        if (prestamo == null) {
            return false;
        }
        if (prestamo.getReserva() != null && prestamo.getReserva().getIdReserva() != null) {
            return incidenciaRepository.contarRetrasosPorReserva(prestamo.getReserva().getIdReserva()) > 0;
        }
        return incidenciaRepository.existsByPrestamoIdPrestamo(prestamo.getIdPrestamo());
    }

    private boolean esDevolucionTardia(Prestamo prestamo, LocalDate fechaDevolucionReal, LocalTime horaDevolucionReal) {
        LocalDateTime fechaHoraLimite = calcularFechaHoraFinPrestamo(prestamo);
        LocalDateTime fechaHoraReal = LocalDateTime.of(fechaDevolucionReal, horaDevolucionReal);
        return fechaHoraReal.isAfter(fechaHoraLimite);
    }

    private void cancelarReservaActiva(Reserva reserva) {
        if (!reserva.estaActiva()) {
            throw new ReservaValidationException(List.of("Solo se pueden cancelar reservas activas."));
        }

        reservaRepository.actualizarEstado(reserva.getIdReserva(), Reserva.ESTADO_CANCELADA);
        reservaDetalleRepository.actualizarEstadoPorReserva(reserva.getIdReserva(), ReservaDetalle.ESTADO_CANCELADA);
        liberarEjemplaresReservados(reserva);
    }

    private void liberarEjemplaresReservados(Reserva reserva) {
        for (ReservaDetalle detalle : reserva.getDetalles()) {
            if (detalle.getLibro() != null) {
                libroRepository.liberarEjemplarReservado(detalle.getLibro().getIdLibro());
            }
        }
    }

    private void vencerReservaSiCorresponde(Reserva reserva) {
        if (!reserva.estaActiva() || !reservaEstaFueraDeTiempo(reserva)) {
            return;
        }

        reservaRepository.actualizarEstado(reserva.getIdReserva(), Reserva.ESTADO_VENCIDA);
        reservaDetalleRepository.actualizarEstadoPorReserva(reserva.getIdReserva(), ReservaDetalle.ESTADO_VENCIDA);
        liberarEjemplaresReservados(reserva);
        reserva.setEstado(Reserva.ESTADO_VENCIDA);
        reserva.getDetalles().forEach(detalle -> detalle.setEstadoDetalleReserva(ReservaDetalle.ESTADO_VENCIDA));
    }

    private boolean reservaEstaFueraDeTiempo(Reserva reserva) {
        LocalDate fechaLimite = reserva.getFechaRecojoLimite() != null
                ? reserva.getFechaRecojoLimite()
                : reserva.getFechaReserva();
        LocalTime horaLimite = reserva.getHoraRecojoLimite();
        if (fechaLimite == null || horaLimite == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(LocalDateTime.of(fechaLimite, horaLimite));
    }

    private List<Integer> validarDatosBase(Integer idUsuario, List<Integer> idsLibro, LocalDate fechaReserva, LocalTime horaReserva,
            Integer horasPrestamo) {
        if (idUsuario == null) {
            throw new ReservaValidationException(List.of("Debes iniciar sesion para reservar un libro."));
        }
        if (idsLibro == null || idsLibro.isEmpty()) {
            throw new ReservaValidationException(List.of("Debe seleccionar al menos un libro."));
        }
        List<Integer> idsLibroUnicos = new ArrayList<>(new LinkedHashSet<>(idsLibro));
        if (idsLibroUnicos.stream().anyMatch(idLibro -> idLibro == null)) {
            throw new ReservaValidationException(List.of("Debe seleccionar libros validos."));
        }
        if (idsLibroUnicos.size() > LIBROS_POR_RESERVA_MAXIMO) {
            throw new ReservaValidationException(List.of("Solo puedes reservar como maximo 3 libros por reserva."));
        }
        validarFechaHoraYDuracion(fechaReserva, horaReserva, horasPrestamo);
        return idsLibroUnicos;
    }

    private void validarFechaHoraYDuracion(LocalDate fechaReserva, LocalTime horaReserva, Integer horasPrestamo) {
        if (fechaReserva == null) {
            throw new ReservaValidationException(List.of("Debe seleccionar una fecha para la reserva."));
        }
        if (horaReserva == null) {
            throw new ReservaValidationException(List.of("Debe seleccionar una hora para la reserva."));
        }
        if (horasPrestamo == null) {
            throw new ReservaValidationException(List.of("Debe indicar cuantas horas solicitas para el libro."));
        }
        if (horasPrestamo < HORAS_PRESTAMO_MINIMO || horasPrestamo > HORAS_PRESTAMO_MAXIMO) {
            throw new ReservaValidationException(List.of("La reserva puede solicitar como maximo 3 horas."));
        }
        if (fechaReserva.isBefore(LocalDate.now())) {
            throw new ReservaValidationException(List.of("La fecha de reserva no puede ser anterior a hoy."));
        }
        if (horaReserva.isBefore(HORA_APERTURA) || horaReserva.isAfter(HORA_CIERRE)) {
            throw new ReservaValidationException(List.of("La hora de reserva debe estar entre 08:00 a.m. y 07:00 p.m."));
        }
        if (fechaReserva.equals(LocalDate.now()) && horaReserva.isBefore(LocalTime.now())) {
            throw new ReservaValidationException(List.of("La hora de reserva no puede ser anterior a la hora actual."));
        }
        LocalTime horaFin = horaReserva.plusHours(horasPrestamo);
        if (horaFin.isAfter(HORA_CIERRE) || horaFin.equals(LocalTime.MIDNIGHT)) {
            throw new ReservaValidationException(
                    List.of("La reserva debe terminar como maximo a las 07:00 p.m. Ajusta la hora o el tiempo solicitado."));
        }
    }

    private LocalDateTime calcularFechaHoraLimite(LocalDate fechaReserva, LocalTime horaReserva, Integer horasPrestamo) {
        return LocalDateTime.of(fechaReserva, horaReserva).plusHours(horasPrestamo);
    }

    private void agregarDatosPrestamo(Reserva reserva) {
        List<Prestamo> prestamos = prestamoRepository.findByReservaIdReservaOrderByIdPrestamoAsc(reserva.getIdReserva());
        prestamos.forEach(prestamo -> prestamo.setIncidenciaRegistrada(
                incidenciaRepository.existsByPrestamoIdPrestamo(prestamo.getIdPrestamo())));
        if (!prestamos.isEmpty() && incidenciaRepository.contarRetrasosPorReserva(reserva.getIdReserva()) > 0) {
            prestamos.forEach(prestamo -> prestamo.setIncidenciaRegistrada(true));
        }
        reserva.setPrestamos(prestamos);
        reserva.setDisponibleParaPrestar(estaDisponibleParaPrestar(reserva));
        reserva.setEstadoGestion(obtenerEstadoGestion(reserva, prestamos));
        if (Reserva.ESTADO_VENCIDA.equals(reserva.getEstado())) {
            reserva.setTiempoRestantePrestamo("Vencida");
            return;
        }
        if (prestamos.isEmpty() || !Reserva.ESTADO_PRESTADA.equals(reserva.getEstado())) {
            reserva.setTiempoRestantePrestamo("-");
            return;
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fin = calcularFechaHoraFinPrestamo(prestamos.get(0));
        Duration restante = Duration.between(ahora, fin);
        if (restante.isNegative() || restante.isZero()) {
            reserva.setTiempoRestantePrestamo("Vencido");
            return;
        }

        long horas = restante.toHours();
        long minutos = restante.toMinutesPart();
        reserva.setTiempoRestantePrestamo(horas + " h " + minutos + " min");
    }

    private LocalDateTime calcularFechaHoraFinPrestamo(Prestamo prestamo) {
        LocalDate fechaFin = prestamo.getFechaPrestamo();
        if (prestamo.getHoraInicio() != null
                && prestamo.getHoraFin() != null
                && !prestamo.getHoraFin().isAfter(prestamo.getHoraInicio())) {
            fechaFin = fechaFin.plusDays(1);
        }
        return LocalDateTime.of(fechaFin, prestamo.getHoraFin());
    }

    private int calcularHorasTotalesPrestamo(Prestamo prestamo, LocalDateTime fechaHoraFin) {
        LocalDateTime fechaHoraInicio = LocalDateTime.of(prestamo.getFechaPrestamo(), prestamo.getHoraInicio());
        long minutos = Duration.between(fechaHoraInicio, fechaHoraFin).toMinutes();
        return (int) Math.ceil(minutos / 60.0);
    }

    private boolean estaDisponibleParaPrestar(Reserva reserva) {
        if (reserva == null || !reserva.estaActiva() || reserva.getFechaReserva() == null || reserva.getHoraReserva() == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(LocalDateTime.of(reserva.getFechaReserva(), reserva.getHoraReserva()));
    }

    private String obtenerEstadoGestion(Reserva reserva, List<Prestamo> prestamos) {
        if (Reserva.ESTADO_CANCELADA.equals(reserva.getEstado())) {
            return "CANCELADO";
        }
        if (Reserva.ESTADO_VENCIDA.equals(reserva.getEstado())) {
            return "VENCIDA";
        }
        if (Reserva.ESTADO_DEVUELTA.equals(reserva.getEstado())) {
            if (prestamos.stream().anyMatch(prestamo -> Prestamo.ESTADO_DEVUELTO_TARDIO.equals(prestamo.getEstado()))) {
                return "DEVUELTO TARDIO";
            }
            return "DEVUELTO";
        }
        if (Reserva.ESTADO_PRESTADA.equals(reserva.getEstado())) {
            if (prestamos.stream().anyMatch(prestamo -> Prestamo.ESTADO_ACTIVO.equals(prestamo.getEstado()))) {
                return "PRESTAMO ACTIVO";
            }
            if (prestamos.stream().anyMatch(prestamo -> Prestamo.ESTADO_DEVUELTO_TARDIO.equals(prestamo.getEstado()))) {
                return "DEVUELTO TARDIO";
            }
            if (prestamos.stream().anyMatch(prestamo -> Prestamo.ESTADO_DEVUELTO.equals(prestamo.getEstado()))) {
                return "DEVUELTO";
            }
            if (prestamos.stream().anyMatch(prestamo -> Prestamo.ESTADO_CANCELADO.equals(prestamo.getEstado()))) {
                return "CANCELADO";
            }
            return "PRESTADA";
        }
        if (reserva.isDisponibleParaPrestar()) {
            return "LISTA PARA PRESTAR";
        }
        return "RESERVA PROGRAMADA";
    }
}
