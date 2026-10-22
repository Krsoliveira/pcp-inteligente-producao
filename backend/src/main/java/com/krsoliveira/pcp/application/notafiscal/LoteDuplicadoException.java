package com.krsoliveira.pcp.application.notafiscal;

/** O fornecedor já entregou um lote com este número para o material. */
public class LoteDuplicadoException extends RuntimeException {
    public LoteDuplicadoException(String numeroLote, String codigoMaterial, String fornecedor) {
        super("O lote '%s' do material %s já foi recebido do fornecedor '%s'."
                .formatted(numeroLote, codigoMaterial, fornecedor));
    }
}
