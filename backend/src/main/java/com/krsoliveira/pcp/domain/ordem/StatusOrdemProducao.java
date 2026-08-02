package com.krsoliveira.pcp.domain.ordem;

/**
 * Ciclo de vida de uma ordem de produção — uma máquina de estados.
 *
 * Fluxo normal:  PLANEJADA -> LIBERADA -> EM_PRODUCAO -> CONCLUIDA
 * Cancelamento:  permitido em qualquer estado, exceto depois de concluída.
 *
 * Centralizar as transições aqui garante que NENHUM ponto do sistema
 * consiga pular etapas (ex.: concluir uma ordem que nunca entrou em produção).
 */
public enum StatusOrdemProducao {

    /** Criada pelo planejador; ainda pode ser ajustada. */
    PLANEJADA,

    /** Aprovada para execução; materiais e recursos reservados. */
    LIBERADA,

    /** Em execução no chão de fábrica. */
    EM_PRODUCAO,

    /** Produção finalizada. Estado terminal. */
    CONCLUIDA,

    /** Abortada antes da conclusão. Estado terminal. */
    CANCELADA;

    /**
     * Diz se a transição deste status para {@code novo} é permitida.
     */
    public boolean podeTransicionarPara(StatusOrdemProducao novo) {
        return switch (this) {
            case PLANEJADA -> novo == LIBERADA || novo == CANCELADA;
            case LIBERADA -> novo == EM_PRODUCAO || novo == CANCELADA;
            case EM_PRODUCAO -> novo == CONCLUIDA || novo == CANCELADA;
            case CONCLUIDA, CANCELADA -> false; // estados terminais
        };
    }

    /**
     * Uma ordem "aberta" ainda não chegou a um estado terminal —
     * é o universo relevante para análise de atraso.
     */
    public boolean estaAberta() {
        return this != CONCLUIDA && this != CANCELADA;
    }
}
