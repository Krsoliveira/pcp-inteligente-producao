package com.krsoliveira.pcp.application.acesso;

import java.util.UUID;

/** A infraestrutura web converte em HTTP 404 (Not Found). */
public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
