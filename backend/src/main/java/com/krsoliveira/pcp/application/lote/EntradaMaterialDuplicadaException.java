package com.krsoliveira.pcp.application.lote;

/**
 * A mesma nota fiscal do mesmo fornecedor já deu entrada neste material.
 */
public class EntradaMaterialDuplicadaException extends RuntimeException {
    public EntradaMaterialDuplicadaException(String notaFiscal, String fornecedor) {
        super("A nota fiscal '%s' do fornecedor '%s' já foi registrada para este material."
                .formatted(notaFiscal, fornecedor));
    }
}
