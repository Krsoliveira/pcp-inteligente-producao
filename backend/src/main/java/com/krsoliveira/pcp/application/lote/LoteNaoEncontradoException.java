package com.krsoliveira.pcp.application.lote;

import java.util.UUID;

public class LoteNaoEncontradoException extends RuntimeException {

    public LoteNaoEncontradoException(UUID id) {
        super("Lote não encontrado: %s.".formatted(id));
    }
}
