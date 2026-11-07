package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.ConflitoException;

/** O fornecedor já entregou um lote com este número para o material. */
public class LoteDuplicadoException extends ConflitoException {
    public LoteDuplicadoException(String numeroLote, String codigoMaterial, String fornecedor) {
        super("O lote '%s' do material %s já foi recebido do fornecedor '%s'."
                .formatted(numeroLote, codigoMaterial, fornecedor));
    }
}
