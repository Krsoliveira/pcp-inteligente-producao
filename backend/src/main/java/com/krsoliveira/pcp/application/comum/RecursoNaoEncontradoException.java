package com.krsoliveira.pcp.application.comum;

/** O registro pedido não existe. A infraestrutura web responde 404. */
public abstract class RecursoNaoEncontradoException extends RuntimeException {

    protected RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
