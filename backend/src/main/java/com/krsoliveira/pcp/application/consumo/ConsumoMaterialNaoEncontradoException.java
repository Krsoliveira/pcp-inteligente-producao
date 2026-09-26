package com.krsoliveira.pcp.application.consumo;

import java.util.UUID;

public class ConsumoMaterialNaoEncontradoException extends RuntimeException {

    public ConsumoMaterialNaoEncontradoException(UUID id) {
        super("Consumo de material não encontrado: %s.".formatted(id));
    }
}
