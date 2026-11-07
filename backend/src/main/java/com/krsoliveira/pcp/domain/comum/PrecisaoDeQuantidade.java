package com.krsoliveira.pcp.domain.comum;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.math.BigDecimal;

/**
 * As quantidades são guardadas com 4 casas decimais. Valor mais preciso é recusado aqui: o banco arredondaria em
 * silêncio, e a soma das alocações deixaria de bater com o consumo registrado.
 */
public final class PrecisaoDeQuantidade {

    public static final int CASAS_DECIMAIS = 4;

    private PrecisaoDeQuantidade() {
    }

    /** @param rotulo sujeito da mensagem de erro, como "A quantidade consumida" */
    public static void exigir(BigDecimal quantidade, String rotulo) {
        if (quantidade != null && quantidade.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw new RegraDeNegocioException("%s aceita no máximo %d casas decimais.".formatted(rotulo, CASAS_DECIMAIS));
        }
    }
}
