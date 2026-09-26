package com.krsoliveira.pcp.domain.lista;

/**
 * Ciclo de vida de uma {@link ListaTecnica}.
 *
 * Transições válidas:
 *   EM_REVISAO → ATIVA    (ao ativar a versão)
 *   ATIVA      → OBSOLETA (ao ativar uma versão mais nova)
 *
 * Não há retorno: uma lista nunca volta de OBSOLETA para EM_REVISAO.
 */
public enum StatusListaTecnica {
    EM_REVISAO,
    ATIVA,
    OBSOLETA;

    public boolean podeSerAtivada() {
        return this == EM_REVISAO;
    }

    public boolean podeSerObsoletada() {
        return this == ATIVA;
    }
}