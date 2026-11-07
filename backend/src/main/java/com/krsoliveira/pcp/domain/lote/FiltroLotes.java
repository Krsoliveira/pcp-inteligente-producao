package com.krsoliveira.pcp.domain.lote;

import java.util.UUID;

/**
 * Critérios de pesquisa de lotes. Todos são opcionais.
 *
 * @param busca      trecho do número do lote
 * @param status     somente lotes neste status
 * @param materialId somente lotes deste material
 */
public record FiltroLotes(String busca, StatusLote status, UUID materialId) {

    static final int TAMANHO_MAXIMO_DA_BUSCA = 100;

    public FiltroLotes {
        if (busca == null || busca.isBlank()) {
            busca = null;
        } else {
            busca = busca.trim();
            if (busca.length() > TAMANHO_MAXIMO_DA_BUSCA) {
                busca = busca.substring(0, TAMANHO_MAXIMO_DA_BUSCA);
            }
        }
    }
}
