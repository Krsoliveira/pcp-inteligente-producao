package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.ConflitoException;

/** Já existe um perfil com esse nome. A infraestrutura web converte em HTTP 409 (Conflict). */
public class PerfilNomeJaUtilizadoException extends ConflitoException {

    public PerfilNomeJaUtilizadoException(String nome) {
        super("Já existe um perfil com o nome \"%s\".".formatted(nome));
    }
}
