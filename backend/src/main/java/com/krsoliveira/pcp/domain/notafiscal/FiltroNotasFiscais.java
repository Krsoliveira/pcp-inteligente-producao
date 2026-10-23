package com.krsoliveira.pcp.domain.notafiscal;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.LocalDate;

/**
 * Critérios de pesquisa de notas fiscais de entrada. Todos são opcionais.
 *
 * @param busca trecho do número da nota, do fornecedor ou do número de um lote que a nota trouxe
 * @param de    recebida a partir desta data (inclusive)
 * @param ate   recebida até esta data (inclusive)
 */
public record FiltroNotasFiscais(String busca, LocalDate de, LocalDate ate) {

    static final int TAMANHO_MAXIMO_DA_BUSCA = 100;

    public FiltroNotasFiscais {
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

    public static FiltroNotasFiscais semFiltro() {
        return new FiltroNotasFiscais(null, null, null);
    }
}
