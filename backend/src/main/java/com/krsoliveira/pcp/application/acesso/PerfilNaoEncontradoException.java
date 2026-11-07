package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

/** A infraestrutura web converte em HTTP 404 (Not Found). */
public class PerfilNaoEncontradoException extends RecursoNaoEncontradoException {

    public PerfilNaoEncontradoException(UUID id) {
        super("Perfil de acesso não encontrado: " + id);
    }
}
