package com.krsoliveira.pcp.application.ordem;

/**
 * Lançada ao tentar criar uma ordem com código já existente.
 * A infraestrutura web converte em HTTP 409 (Conflict).
 */
public class CodigoJaUtilizadoException extends RuntimeException {

    public CodigoJaUtilizadoException(String codigo) {
        super("Já existe uma ordem de produção com o código '" + codigo + "'.");
    }
}
