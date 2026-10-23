package com.krsoliveira.pcp.domain.acesso;

/**
 * Lançada quando o usuário autenticado não pode fazer o que pediu (ex.: conceder uma permissão
 * que ele mesmo não tem). A infraestrutura web converte em HTTP 403 (Forbidden).
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
