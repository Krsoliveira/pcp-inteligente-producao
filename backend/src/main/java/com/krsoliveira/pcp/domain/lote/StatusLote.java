package com.krsoliveira.pcp.domain.lote;

/**
 * Ciclo de vida de um lote de produção.
 *
 * DISPONIVEL  — lote recém-gerado, pronto para uso ou expedição.
 * BLOQUEADO   — retido por qualidade, aguardando liberação.
 * CONSUMIDO   — totalmente utilizado como insumo de outra ordem.
 * VENCIDO     — data de validade expirada (transição por processo agendado).
 */
public enum StatusLote {

    DISPONIVEL,
    BLOQUEADO,
    CONSUMIDO,
    VENCIDO
}
