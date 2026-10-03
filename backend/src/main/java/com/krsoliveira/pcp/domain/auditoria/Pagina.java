package com.krsoliveira.pcp.domain.auditoria;

import java.util.List;

/** Fatia de um resultado paginado. {@code pagina} começa em 0. */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {

    public int totalPaginas() {
        return tamanho == 0 ? 0 : (int) Math.ceil((double) total / tamanho);
    }
}
