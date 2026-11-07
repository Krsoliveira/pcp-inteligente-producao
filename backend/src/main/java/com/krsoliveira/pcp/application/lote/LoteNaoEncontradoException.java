package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class LoteNaoEncontradoException extends RecursoNaoEncontradoException {

    public LoteNaoEncontradoException(UUID id) {
        super("Lote não encontrado: %s.".formatted(id));
    }
}
