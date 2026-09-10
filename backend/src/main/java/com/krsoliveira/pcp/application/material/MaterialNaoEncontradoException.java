package com.krsoliveira.pcp.application.material;

import java.util.UUID;

public class MaterialNaoEncontradoException extends RuntimeException {
    public MaterialNaoEncontradoException(UUID id) {
        super("Material não encontrado: %s.".formatted(id));
    }
}