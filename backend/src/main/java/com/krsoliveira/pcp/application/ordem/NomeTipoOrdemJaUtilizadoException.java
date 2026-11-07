package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.ConflitoException;

public class NomeTipoOrdemJaUtilizadoException extends ConflitoException {

    public NomeTipoOrdemJaUtilizadoException(String nome) {
        super("Já existe um tipo de ordem com o nome '%s'.".formatted(nome));
    }
}
