package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class TipoOrdemNaoEncontradoException extends RecursoNaoEncontradoException {

    public TipoOrdemNaoEncontradoException(UUID id) {
        super("Tipo de ordem não encontrado: %s.".formatted(id));
    }
}
