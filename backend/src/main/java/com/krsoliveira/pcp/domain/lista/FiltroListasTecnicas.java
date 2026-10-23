package com.krsoliveira.pcp.domain.lista;

import java.util.UUID;

/**
 * Critérios de pesquisa de listas técnicas. Ambos são opcionais.
 *
 * @param materialId somente versões deste material
 * @param status     somente listas neste status
 */
public record FiltroListasTecnicas(UUID materialId, StatusListaTecnica status) {

    public static FiltroListasTecnicas semFiltro() {
        return new FiltroListasTecnicas(null, null);
    }
}
