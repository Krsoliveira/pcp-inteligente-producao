package com.krsoliveira.pcp.application.material;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class MaterialNaoEncontradoException extends RecursoNaoEncontradoException {
    public MaterialNaoEncontradoException(UUID id) {
        super("Material não encontrado: %s.".formatted(id));
    }
}