package com.krsoliveira.pcp.domain.ordem;

/**
 * Critérios de pesquisa de ordens. Ambos são opcionais.
 *
 * @param busca  trecho do código da ordem (sem diferenciar maiúsculas); em branco vira {@code null}
 * @param status somente ordens neste status
 */
public record FiltroOrdens(String busca, StatusOrdemProducao status) {

    /** Código tem no máximo 30 caracteres; mais que isso nunca casa, então não vale processar. */
    static final int TAMANHO_MAXIMO_DA_BUSCA = 100;

    public FiltroOrdens {
        if (busca == null || busca.isBlank()) {
            busca = null;
        } else {
            busca = busca.trim();
            if (busca.length() > TAMANHO_MAXIMO_DA_BUSCA) {
                busca = busca.substring(0, TAMANHO_MAXIMO_DA_BUSCA);
            }
        }
    }

    public static FiltroOrdens semFiltro() {
        return new FiltroOrdens(null, null);
    }
}
