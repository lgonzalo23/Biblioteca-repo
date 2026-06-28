package com.luzdelsaber.biblioteca.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.luzdelsaber.biblioteca.dto.AutorForm;
import com.luzdelsaber.biblioteca.dto.CategoriaForm;
import com.luzdelsaber.biblioteca.dto.LibroForm;
import com.luzdelsaber.biblioteca.exception.BibliografiaValidationException;
import com.luzdelsaber.biblioteca.model.Autor;
import com.luzdelsaber.biblioteca.model.Categoria;
import com.luzdelsaber.biblioteca.model.Libro;
import com.luzdelsaber.biblioteca.repository.AutorRepository;
import com.luzdelsaber.biblioteca.repository.CategoriaRepository;
import com.luzdelsaber.biblioteca.repository.LibroRepository;

@Service
public class BibliografiaService {

    private static final int STOCK_MAXIMO = 9999;
    private static final Pattern TEXTO_SOLO_LETRAS = Pattern.compile("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü\\s.'-]+$");
    private static final Pattern ISBN_VALIDO = Pattern.compile("^[A-Za-z0-9-]+$");
    private static final Pattern URL_IMAGEN_VALIDA = Pattern.compile("^https?://.+");

    private static final Set<String> ESTADOS_LIBRO = Set.of(
            Libro.ESTADO_DISPONIBLE,
            Libro.ESTADO_PRESTADO,
            Libro.ESTADO_RESERVADO,
            Libro.ESTADO_NO_DISPONIBLE);

    private final LibroRepository libroRepository;
    private final CategoriaRepository categoriaRepository;
    private final AutorRepository autorRepository;

    public BibliografiaService(
            LibroRepository libroRepository,
            CategoriaRepository categoriaRepository,
            AutorRepository autorRepository) {
        this.libroRepository = libroRepository;
        this.categoriaRepository = categoriaRepository;
        this.autorRepository = autorRepository;
    }

    public List<Libro> listarLibros(String termino) {
        if (StringUtils.hasText(termino)) {
            return libroRepository.buscar(termino.trim());
        }
        return libroRepository.listarOrdenado();
    }

    public List<Libro> listarLibrosCatalogo() {
        return listarLibrosCatalogo(null, null);
    }

    public List<Libro> listarLibrosCatalogo(String termino) {
        return listarLibrosCatalogo(termino, null);
    }

    public List<Libro> listarLibrosCatalogo(String termino, Integer categoriaId) {
        if (StringUtils.hasText(termino)) {
            return libroRepository.buscarCatalogo(termino.trim(), categoriaId);
        }
        return libroRepository.listarCatalogo(categoriaId);
    }

    public List<Categoria> listarCategorias() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    public List<Categoria> listarCategorias(String termino) {
        if (StringUtils.hasText(termino)) {
            return categoriaRepository.buscarEnUnaTabla(termino.trim());
        }
        return listarCategorias();
    }

    public List<Categoria> listarCategoriasActivas() {
        return listarCategorias()
                .stream()
                .filter(categoria -> Categoria.ESTADO_ACTIVO.equals(categoria.getEstado()))
                .toList();
    }

    public List<Autor> listarAutores() {
        return autorRepository.findAllByOrderByApellidoAscNombreAsc();
    }

    public List<Autor> listarAutores(String termino) {
        if (StringUtils.hasText(termino)) {
            return autorRepository.buscarEnUnaTabla(termino.trim());
        }
        return listarAutores();
    }

    public List<Autor> listarAutoresActivos() {
        return listarAutores()
                .stream()
                .filter(autor -> Autor.ESTADO_ACTIVO.equals(autor.getEstado()))
                .toList();
    }

    public Optional<Libro> buscarLibroPorId(Integer idLibro) {
        return libroRepository.findById(idLibro);
    }

    @Transactional
    public Libro crearLibro(LibroForm form) {
        normalizarLibro(form);
        validarLibro(form, null);

        Libro libro = new Libro();
        aplicarDatosLibro(libro, form);
        libroRepository.insertarLibro(
                libro.getIdCategoria(),
                libro.getTitulo(),
                libro.getIsbn(),
                libro.getFechaPublicacion(),
                libro.getEstado(),
                libro.getStock(),
                libro.getUbicacion(),
                libro.getUrlImagen());

        Libro libroRegistrado = libroRepository.findByIsbnIgnoreCase(libro.getIsbn())
                .orElseThrow(() -> new BibliografiaValidationException(List.of("No se pudo recuperar el libro registrado.")));
        guardarAutoresLibro(libroRegistrado.getIdLibro(), libro.getAutores());
        return libroRegistrado;
    }

    @Transactional
    public Libro actualizarLibro(Integer idLibro, LibroForm form) {
        normalizarLibro(form);
        Libro libro = libroRepository.findById(idLibro)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("El libro seleccionado no existe.")));
        form.setIsbn(libro.getIsbn());
        validarLibro(form, idLibro);

        Categoria categoria = obtenerCategoria(form.getCategoriaId());
        List<Autor> autores = obtenerAutores(form.getAutorIds());
        libroRepository.actualizarLibro(
                idLibro,
                categoria.getIdCategoria(),
                form.getTitulo(),
                form.getFechaPublicacion(),
                form.getEstado(),
                form.getStock(),
                form.getUbicacion(),
                form.getUrlImagen());
        guardarAutoresLibro(idLibro, autores);
        return libro;
    }

    @Transactional
    public void eliminarLibroFisico(Integer idLibro) {
        if (!libroRepository.existsById(idLibro)) {
            throw new BibliografiaValidationException(List.of("El libro seleccionado no existe."));
        }
        libroRepository.eliminarAutoresDelLibro(idLibro);
        libroRepository.eliminarFisico(idLibro);
    }

    @Transactional
    public void eliminarLibroLogico(Integer idLibro) {
        if (!libroRepository.existsById(idLibro)) {
            throw new BibliografiaValidationException(List.of("El libro seleccionado no existe."));
        }
        libroRepository.actualizarEstado(idLibro, Libro.ESTADO_NO_DISPONIBLE);
    }

    @Transactional
    public Categoria crearCategoria(CategoriaForm form) {
        normalizarCategoria(form);
        validarCategoria(form);

        Categoria categoria = new Categoria();
        categoria.setNombre(form.getNombre());
        categoria.setDescripcion(form.getDescripcion());
        categoria.setEstado(Categoria.ESTADO_ACTIVO);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Categoria actualizarCategoria(Integer idCategoria, CategoriaForm form) {
        normalizarCategoria(form);
        validarCategoria(form);

        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("La categoria seleccionada no existe.")));
        categoria.actualizarCategoria(form.getNombre());
        categoria.setDescripcion(form.getDescripcion());
        categoria.setEstado(form.getEstado());
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void eliminarCategoriaLogico(Integer idCategoria) {
        Categoria categoria = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("La categoria seleccionada no existe.")));
        categoria.setEstado(Categoria.ESTADO_INACTIVO);
        categoriaRepository.save(categoria);
    }

    @Transactional
    public void eliminarCategoriaFisico(Integer idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new BibliografiaValidationException(List.of("La categoria seleccionada no existe."));
        }
        categoriaRepository.deleteById(idCategoria);
    }

    @Transactional
    public Autor crearAutor(AutorForm form) {
        normalizarAutor(form);
        validarAutor(form);

        Autor autor = new Autor();
        autor.setNombre(form.getNombre());
        autor.setApellido(form.getApellido());
        autor.setNacionalidad(form.getNacionalidad());
        autor.setEstado(Autor.ESTADO_ACTIVO);
        return autorRepository.save(autor);
    }

    @Transactional
    public Autor actualizarAutor(Integer idAutor, AutorForm form) {
        normalizarAutor(form);
        validarAutor(form);

        Autor autor = autorRepository.findById(idAutor)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("El autor seleccionado no existe.")));
        autor.actualizarAutor(form.getNombre());
        autor.setApellido(form.getApellido());
        autor.setNacionalidad(form.getNacionalidad());
        autor.setEstado(form.getEstado());
        return autorRepository.save(autor);
    }

    @Transactional
    public void eliminarAutorLogico(Integer idAutor) {
        Autor autor = autorRepository.findById(idAutor)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("El autor seleccionado no existe.")));
        autor.setEstado(Autor.ESTADO_INACTIVO);
        autorRepository.save(autor);
    }

    @Transactional
    public void eliminarAutorFisico(Integer idAutor) {
        if (!autorRepository.existsById(idAutor)) {
            throw new BibliografiaValidationException(List.of("El autor seleccionado no existe."));
        }
        autorRepository.deleteById(idAutor);
    }

    private void aplicarDatosLibro(Libro libro, LibroForm form) {
        Categoria categoria = obtenerCategoria(form.getCategoriaId());
        List<Autor> autores = obtenerAutores(form.getAutorIds());

        libro.setCategoria(categoria);
        libro.setTitulo(form.getTitulo());
        libro.setIsbn(form.getIsbn());
        libro.setFechaPublicacion(form.getFechaPublicacion());
        libro.setEstado(form.getEstado());
        libro.setStock(form.getStock());
        libro.setUbicacion(form.getUbicacion());
        libro.setUrlImagen(form.getUrlImagen());
        libro.setAutores(autores);
    }

    private Categoria obtenerCategoria(Integer categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new BibliografiaValidationException(List.of("La categoria seleccionada no existe.")));
    }

    private List<Autor> obtenerAutores(List<Integer> autorIds) {
        return autorRepository.findAllById(autorIds);
    }

    private void guardarAutoresLibro(Integer idLibro, List<Autor> autores) {
        libroRepository.eliminarAutoresDelLibro(idLibro);
        for (Autor autor : autores) {
            libroRepository.insertarAutorDelLibro(idLibro, autor.getIdAutor());
        }
    }

    private void validarLibro(LibroForm form, Integer idActual) {
        List<String> errores = new ArrayList<>();

        validarTextoObligatorio(form.getTitulo(), "El titulo", 150, errores);
        validarTextoObligatorio(form.getIsbn(), "El ISBN", 20, errores);
        validarTextoObligatorio(form.getUbicacion(), "La ubicacion", 50, errores);
        validarTextoOpcional(form.getUrlImagen(), "El enlace de imagen", 255, errores);
        validarIsbnFormato(form.getIsbn(), errores);
        validarUrlImagen(form.getUrlImagen(), errores);

        if (form.getStock() == null || form.getStock() < 0) {
            errores.add("El stock debe ser un numero entero mayor o igual a 0.");
        } else if (form.getStock() > STOCK_MAXIMO) {
            errores.add("El stock no puede superar " + STOCK_MAXIMO + " unidades.");
        }

        if (form.getFechaPublicacion() != null && form.getFechaPublicacion().isAfter(java.time.LocalDate.now())) {
            errores.add("La fecha de publicacion no puede ser futura.");
        }

        if (!ESTADOS_LIBRO.contains(form.getEstado())) {
            errores.add("El estado debe ser DISPONIBLE, PRESTADO, RESERVADO o NO_DISPONIBLE.");
        }

        if (form.getCategoriaId() == null || !categoriaRepository.existsById(form.getCategoriaId())) {
            errores.add("Debe seleccionar una categoria valida.");
        } else if (idActual == null) {
            categoriaRepository.findById(form.getCategoriaId()).ifPresent(categoria -> {
                if (!Categoria.ESTADO_ACTIVO.equals(categoria.getEstado())) {
                    errores.add("Debe seleccionar una categoria activa.");
                }
            });
        }

        validarAutores(form.getAutorIds(), idActual == null, errores);
        validarIsbnUnico(form.getIsbn(), idActual, errores);

        if (!errores.isEmpty()) {
            throw new BibliografiaValidationException(errores);
        }
    }

    private void validarAutores(List<Integer> autorIds, boolean soloActivos, List<String> errores) {
        if (autorIds == null || autorIds.isEmpty()) {
            errores.add("Debe seleccionar al menos un autor.");
            return;
        }

        Set<Integer> idsUnicos = new LinkedHashSet<>(autorIds);
        List<Autor> autores = autorRepository.findAllById(idsUnicos);
        if (autores.size() != idsUnicos.size()) {
            errores.add("Uno o mas autores seleccionados no existen.");
        }
        if (soloActivos && autores.stream().anyMatch(autor -> !Autor.ESTADO_ACTIVO.equals(autor.getEstado()))) {
            errores.add("Debe seleccionar solo autores activos.");
        }
    }

    private void validarIsbnUnico(String isbn, Integer idActual, List<String> errores) {
        if (!StringUtils.hasText(isbn)) {
            return;
        }
        libroRepository.findByIsbnIgnoreCase(isbn).ifPresent(existente -> {
            if (!existente.getIdLibro().equals(idActual)) {
                errores.add("El ISBN ya esta registrado.");
            }
        });
    }

    private void validarCategoria(CategoriaForm form) {
        List<String> errores = new ArrayList<>();
        validarTextoObligatorio(form.getNombre(), "El nombre de la categoria", 50, errores);
        validarSoloLetras(form.getNombre(), "El nombre de la categoria", errores);
        validarTextoOpcional(form.getDescripcion(), "La descripcion de la categoria", 150, errores);
        validarEstadoRegistro(form.getEstado(), "El estado de la categoria", errores);
        if (!errores.isEmpty()) {
            throw new BibliografiaValidationException(errores);
        }
    }

    private void validarAutor(AutorForm form) {
        List<String> errores = new ArrayList<>();
        validarTextoObligatorio(form.getNombre(), "El nombre del autor", 50, errores);
        validarTextoObligatorio(form.getApellido(), "El apellido del autor", 50, errores);
        validarTextoOpcional(form.getNacionalidad(), "La nacionalidad", 30, errores);
        validarSoloLetras(form.getNombre(), "El nombre del autor", errores);
        validarSoloLetras(form.getApellido(), "El apellido del autor", errores);
        validarSoloLetrasOpcional(form.getNacionalidad(), "La nacionalidad", errores);
        validarEstadoRegistro(form.getEstado(), "El estado del autor", errores);
        if (!errores.isEmpty()) {
            throw new BibliografiaValidationException(errores);
        }
    }

    private void validarEstadoRegistro(String estado, String campo, List<String> errores) {
        if (!Categoria.ESTADO_ACTIVO.equals(estado) && !Categoria.ESTADO_INACTIVO.equals(estado)) {
            errores.add(campo + " debe ser ACTIVO o INACTIVO.");
        }
    }

    private void validarTextoObligatorio(String valor, String campo, int maximo, List<String> errores) {
        if (!StringUtils.hasText(valor)) {
            errores.add(campo + " es obligatorio.");
            return;
        }
        if (valor.length() > maximo) {
            errores.add(campo + " no puede superar " + maximo + " caracteres.");
        }
    }

    private void validarTextoOpcional(String valor, String campo, int maximo, List<String> errores) {
        if (StringUtils.hasText(valor) && valor.length() > maximo) {
            errores.add(campo + " no puede superar " + maximo + " caracteres.");
        }
    }

    private void validarSoloLetras(String valor, String campo, List<String> errores) {
        if (StringUtils.hasText(valor) && !TEXTO_SOLO_LETRAS.matcher(valor).matches()) {
            errores.add(campo + " solo puede contener letras.");
        }
    }

    private void validarSoloLetrasOpcional(String valor, String campo, List<String> errores) {
        if (StringUtils.hasText(valor) && !TEXTO_SOLO_LETRAS.matcher(valor).matches()) {
            errores.add(campo + " solo puede contener letras.");
        }
    }

    private void validarIsbnFormato(String isbn, List<String> errores) {
        if (StringUtils.hasText(isbn) && !ISBN_VALIDO.matcher(isbn).matches()) {
            errores.add("El ISBN solo puede contener letras, numeros y guiones.");
        }
    }

    private void validarUrlImagen(String urlImagen, List<String> errores) {
        if (StringUtils.hasText(urlImagen) && !URL_IMAGEN_VALIDA.matcher(urlImagen).matches()) {
            errores.add("El enlace de imagen debe iniciar con http:// o https://.");
        }
    }

    private void normalizarLibro(LibroForm form) {
        form.setTitulo(limpiar(form.getTitulo()));
        form.setIsbn(limpiar(form.getIsbn()));
        form.setUbicacion(limpiar(form.getUbicacion()));
        form.setUrlImagen(limpiar(form.getUrlImagen()));
        String estado = StringUtils.hasText(form.getEstado())
                ? form.getEstado().trim().toUpperCase().replace(' ', '_')
                : Libro.ESTADO_DISPONIBLE;
        form.setEstado(estado);
        form.setAutorIds(form.getAutorIds() == null ? List.of() : new ArrayList<>(new LinkedHashSet<>(form.getAutorIds())));
    }

    private void normalizarCategoria(CategoriaForm form) {
        form.setNombre(limpiar(form.getNombre()));
        form.setDescripcion(limpiar(form.getDescripcion()));
        form.setEstado(StringUtils.hasText(form.getEstado()) ? form.getEstado().trim().toUpperCase() : Categoria.ESTADO_ACTIVO);
    }

    private void normalizarAutor(AutorForm form) {
        form.setNombre(limpiar(form.getNombre()));
        form.setApellido(limpiar(form.getApellido()));
        form.setNacionalidad(limpiar(form.getNacionalidad()));
        form.setEstado(StringUtils.hasText(form.getEstado()) ? form.getEstado().trim().toUpperCase() : Autor.ESTADO_ACTIVO);
    }

    private String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
