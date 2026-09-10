package com.krsoliveira.pcp.application.lista;

import java.util.UUID;

public class ListaTecnicaNaoEncontradaException extends RuntimeException {
    public ListaTecnicaNaoEncontradaException(UUID id) {
        super("Lista técnica não encontrada: %s.".formatted(id));
    }
}