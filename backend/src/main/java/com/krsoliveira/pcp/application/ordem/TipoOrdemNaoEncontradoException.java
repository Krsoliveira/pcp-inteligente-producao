package com.krsoliveira.pcp.application.ordem;

import java.util.UUID;

public class TipoOrdemNaoEncontradoException extends RuntimeException {

    public TipoOrdemNaoEncontradoException(UUID id) {
        super("Tipo de ordem não encontrado: %s.".formatted(id));
    }
}
