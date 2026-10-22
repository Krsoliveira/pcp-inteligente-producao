package com.krsoliveira.pcp.domain.estoque;

import com.krsoliveira.pcp.domain.material.FiltroMateriais;

/**
 * Critérios de pesquisa do estoque.
 *
 * @param material       busca por código ou descrição e tipo do material
 * @param somenteComSaldo só materiais com saldo disponível
 */
public record FiltroEstoque(FiltroMateriais material, boolean somenteComSaldo) {

    public static FiltroEstoque semFiltro() {
        return new FiltroEstoque(FiltroMateriais.semFiltro(), false);
    }
}
