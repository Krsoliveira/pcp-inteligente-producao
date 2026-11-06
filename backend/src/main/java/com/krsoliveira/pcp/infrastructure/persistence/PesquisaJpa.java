package com.krsoliveira.pcp.infrastructure.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Apoio às pesquisas com filtro opcional. */
final class PesquisaJpa {

    private PesquisaJpa() {
    }

    /**
     * Monta o padrão "contém" para LIKE com escape {@code '\\'}: %, _ e \\ digitados pela pessoa
     * são texto comum, não curingas.
     */
    static String contem(String texto) {
        String escapado = texto.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }

    static final char ESCAPE = '\\';

    /**
     * Pedido de página para o JPA, que só aceita deslocamento dentro de um inteiro. Página mais alta que isso fica no
     * limite: continua depois da última e devolve a lista vazia, como qualquer outra página além do fim.
     */
    static PageRequest pagina(int pagina, int tamanho, Sort ordenacao) {
        return PageRequest.of(Math.min(pagina, Integer.MAX_VALUE / tamanho), tamanho, ordenacao);
    }
}
