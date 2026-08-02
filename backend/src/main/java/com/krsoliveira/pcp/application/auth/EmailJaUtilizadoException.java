package com.krsoliveira.pcp.application.auth;

/**
 * Lançada quando se tenta registrar um e-mail que já está cadastrado.
 * A infraestrutura web converte em HTTP 409 (Conflict).
 */
public class EmailJaUtilizadoException extends RuntimeException {

    public EmailJaUtilizadoException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
