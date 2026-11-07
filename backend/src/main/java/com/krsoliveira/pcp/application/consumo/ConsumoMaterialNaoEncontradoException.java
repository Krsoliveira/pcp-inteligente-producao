package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class ConsumoMaterialNaoEncontradoException extends RecursoNaoEncontradoException {

    public ConsumoMaterialNaoEncontradoException(UUID id) {
        super("Consumo de material não encontrado: %s.".formatted(id));
    }
}
