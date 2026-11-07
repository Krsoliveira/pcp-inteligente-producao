package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class ListaTecnicaNaoEncontradaException extends RecursoNaoEncontradoException {
    public ListaTecnicaNaoEncontradaException(UUID id) {
        super("Lista técnica não encontrada: %s.".formatted(id));
    }
}