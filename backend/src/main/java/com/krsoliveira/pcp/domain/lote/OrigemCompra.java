package com.krsoliveira.pcp.domain.lote;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.LocalDate;

/**
 * Origem de um lote comprado: de qual fornecedor e nota fiscal ele veio, quando a nota
 * foi emitida e quando o material chegou fisicamente.
 *
 * Value object — um lote de compra sempre tem os quatro dados, e eles nunca mudam.
 */
public record OrigemCompra(String fornecedor, String notaFiscal,
                           LocalDate dataEmissaoNf, LocalDate dataRecebimento) {

    static final int FORNECEDOR_MAX = 150;
    static final int NOTA_FISCAL_MAX = 44;

    public OrigemCompra {
        if (fornecedor == null || fornecedor.isBlank()) {
            throw new RegraDeNegocioException("O fornecedor é obrigatório na entrada de material.");
        }
        fornecedor = fornecedor.trim();
        if (fornecedor.length() > FORNECEDOR_MAX) {
            throw new RegraDeNegocioException(
                    "O fornecedor deve ter no máximo %d caracteres.".formatted(FORNECEDOR_MAX));
        }
        if (notaFiscal == null || notaFiscal.isBlank()) {
            throw new RegraDeNegocioException("A nota fiscal é obrigatória na entrada de material.");
        }
        notaFiscal = notaFiscal.trim();
        if (notaFiscal.length() > NOTA_FISCAL_MAX) {
            throw new RegraDeNegocioException(
                    "A nota fiscal deve ter no máximo %d caracteres.".formatted(NOTA_FISCAL_MAX));
        }
        if (dataEmissaoNf == null) {
            throw new RegraDeNegocioException("A data de emissão da nota fiscal é obrigatória.");
        }
        if (dataRecebimento == null) {
            throw new RegraDeNegocioException("A data de recebimento do material é obrigatória.");
        }
        if (dataRecebimento.isBefore(dataEmissaoNf)) {
            throw new RegraDeNegocioException(
                    "A data de recebimento não pode ser anterior à emissão da nota fiscal.");
        }
    }
}
