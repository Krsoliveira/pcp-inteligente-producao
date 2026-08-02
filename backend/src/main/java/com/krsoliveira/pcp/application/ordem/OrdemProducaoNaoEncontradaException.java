package com.krsoliveira.pcp.application.ordem;

import java.util.UUID;

/**
 * Lançada quando o id informado não corresponde a nenhuma ordem.
 * A infraestrutura web converte em HTTP 404 (Not Found).
 */
public class OrdemProducaoNaoEncontradaException extends RuntimeException {

    public OrdemProducaoNaoEncontradaException(UUID id) {
        super("Ordem de produção não encontrada: " + id);
    }
}
