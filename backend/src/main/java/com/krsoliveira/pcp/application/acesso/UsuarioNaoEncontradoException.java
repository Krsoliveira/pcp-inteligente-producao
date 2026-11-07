package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

/** A infraestrutura web converte em HTTP 404 (Not Found). */
public class UsuarioNaoEncontradoException extends RecursoNaoEncontradoException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
