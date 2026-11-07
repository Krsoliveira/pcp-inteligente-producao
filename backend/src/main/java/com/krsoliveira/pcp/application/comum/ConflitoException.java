package com.krsoliveira.pcp.application.comum;

/** O dado informado já existe (código, nome, e-mail). A infraestrutura web responde 409. */
public abstract class ConflitoException extends RuntimeException {

    protected ConflitoException(String mensagem) {
        super(mensagem);
    }
}
