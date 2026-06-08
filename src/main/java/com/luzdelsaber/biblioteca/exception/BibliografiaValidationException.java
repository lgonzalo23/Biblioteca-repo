package com.luzdelsaber.biblioteca.exception;

import java.util.List;

public class BibliografiaValidationException extends RuntimeException {

    private final List<String> errores;

    public BibliografiaValidationException(List<String> errores) {
        super(String.join(" ", errores));
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
