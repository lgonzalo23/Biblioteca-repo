package com.luzdelsaber.biblioteca.exception;

import java.util.List;

public class ReservaValidationException extends RuntimeException {

    private final List<String> errores;

    public ReservaValidationException(List<String> errores) {
        super(String.join(" ", errores));
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
