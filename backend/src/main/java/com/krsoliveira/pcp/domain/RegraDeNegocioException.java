package com.krsoliveira.pcp.domain;

/**
 * Lançada quando uma regra de negócio do domínio é violada
 * (ex.: quantidade inválida, transição de status não permitida).
 *
 * A infraestrutura web converte esta exceção em HTTP 422 (Unprocessable Entity).
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
