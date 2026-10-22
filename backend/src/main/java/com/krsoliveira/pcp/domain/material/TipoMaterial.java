package com.krsoliveira.pcp.domain.material;

/**
 * Classifica o papel do material no processo produtivo.
 *
 * Regra fundamental: apenas PRODUTO_ACABADO e SEMIACABADO podem ter
 * uma {@link ListaTecnica} associada. MATERIA_PRIMA é insumo folha —
 * não se decompõe em componentes dentro deste sistema.
 *
 * Cada tipo tem sua faixa de código (ADR-0012): os 3 primeiros dos 9 dígitos.
 */
public enum TipoMaterial {
    PRODUTO_ACABADO("103"),
    SEMIACABADO("105"),
    MATERIA_PRIMA("110");

    private final String prefixoCodigo;

    TipoMaterial(String prefixoCodigo) {
        this.prefixoCodigo = prefixoCodigo;
    }

    /** Prefixo da faixa de código: 103 (acabado), 105 (semiacabado), 110 (matéria-prima). */
    public String prefixoCodigo() {
        return prefixoCodigo;
    }

    public boolean podeTermListaTecnica() {
        return this == PRODUTO_ACABADO || this == SEMIACABADO;
    }
}