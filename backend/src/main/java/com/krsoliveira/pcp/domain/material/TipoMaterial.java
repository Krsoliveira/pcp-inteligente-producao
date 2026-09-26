package com.krsoliveira.pcp.domain.material;

/**
 * Classifica o papel do material no processo produtivo.
 *
 * Regra fundamental: apenas PRODUTO_ACABADO e SEMIACABADO podem ter
 * uma {@link ListaTecnica} associada. MATERIA_PRIMA é insumo folha —
 * não se decompõe em componentes dentro deste sistema.
 */
public enum TipoMaterial {
    PRODUTO_ACABADO,
    SEMIACABADO,
    MATERIA_PRIMA;

    public boolean podeTermListaTecnica() {
        return this == PRODUTO_ACABADO || this == SEMIACABADO;
    }
}