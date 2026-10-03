package com.krsoliveira.pcp.application.notafiscal;

import java.util.UUID;

public class NotaFiscalNaoEncontradaException extends RuntimeException {
    public NotaFiscalNaoEncontradaException(UUID id) {
        super("Nota fiscal não encontrada: " + id);
    }
}
