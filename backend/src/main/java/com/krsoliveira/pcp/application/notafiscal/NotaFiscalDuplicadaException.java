package com.krsoliveira.pcp.application.notafiscal;

/** A mesma nota fiscal do mesmo fornecedor já foi registrada. */
public class NotaFiscalDuplicadaException extends RuntimeException {
    public NotaFiscalDuplicadaException(String numero, String fornecedor) {
        super("A nota fiscal '%s' do fornecedor '%s' já foi registrada.".formatted(numero, fornecedor));
    }
}
