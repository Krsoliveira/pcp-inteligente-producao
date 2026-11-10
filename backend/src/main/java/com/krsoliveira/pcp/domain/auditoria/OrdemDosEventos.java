package com.krsoliveira.pcp.domain.auditoria;

/**
 * Ordem em que a trilha de auditoria é devolvida. O padrão é o mais recente primeiro (o que sempre foi); o mais antigo
 * primeiro serve ao histórico lido como o fluxo do registro, do "Criado" ao último evento.
 */
public enum OrdemDosEventos {
    MAIS_RECENTES_PRIMEIRO,
    MAIS_ANTIGOS_PRIMEIRO
}
