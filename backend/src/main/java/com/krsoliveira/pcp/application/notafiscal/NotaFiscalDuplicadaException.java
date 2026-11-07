package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.ConflitoException;

/** A mesma nota fiscal do mesmo fornecedor já foi registrada. */
public class NotaFiscalDuplicadaException extends ConflitoException {
    public NotaFiscalDuplicadaException(String numero, String fornecedor) {
        super("A nota fiscal '%s' do fornecedor '%s' já foi registrada.".formatted(numero, fornecedor));
    }
}
