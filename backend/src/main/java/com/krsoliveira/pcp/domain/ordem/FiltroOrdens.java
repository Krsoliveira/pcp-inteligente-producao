package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.LocalDate;

/**
 * Critérios de pesquisa de ordens. Todos são opcionais.
 *
 * @param busca  trecho do código da ordem (sem diferenciar maiúsculas); em branco vira {@code null}
 * @param status somente ordens neste status
 * @param de     início planejado a partir desta data (inclusive)
 * @param ate    início planejado até esta data (inclusive)
 */
public record FiltroOrdens(String busca, StatusOrdemProducao status, LocalDate de, LocalDate ate) {

    /** Código tem no máximo 30 caracteres; mais que isso nunca casa, então não vale processar. */
    static final int TAMANHO_MAXIMO_DA_BUSCA = 100;

    public FiltroOrdens {
        if (de != null && ate != null && ate.isBefore(de)) {
            throw new RegraDeNegocioException("O fim do período não pode ser anterior ao início.");
        }
        if (busca == null || busca.isBlank()) {
            busca = null;
        } else {
            busca = busca.trim();
            if (busca.length() > TAMANHO_MAXIMO_DA_BUSCA) {
                busca = busca.substring(0, TAMANHO_MAXIMO_DA_BUSCA);
            }
        }
    }

    /** Pesquisa sem período. */
    public FiltroOrdens(String busca, StatusOrdemProducao status) {
        this(busca, status, null, null);
    }

    public static FiltroOrdens semFiltro() {
        return new FiltroOrdens(null, null, null, null);
    }
}
