package com.krsoliveira.pcp.application.notafiscal;

import com.krsoliveira.pcp.application.comum.RecursoNaoEncontradoException;
import java.util.UUID;

public class NotaFiscalNaoEncontradaException extends RecursoNaoEncontradoException {
    public NotaFiscalNaoEncontradaException(UUID id) {
        super("Nota fiscal não encontrada: " + id);
    }
}
