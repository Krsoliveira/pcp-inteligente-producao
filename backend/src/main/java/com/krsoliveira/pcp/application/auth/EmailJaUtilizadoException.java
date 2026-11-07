package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.application.comum.ConflitoException;

/**
 * Lançada quando se tenta registrar um e-mail que já está cadastrado.
 * A infraestrutura web converte em HTTP 409 (Conflict).
 */
public class EmailJaUtilizadoException extends ConflitoException {

    public EmailJaUtilizadoException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
