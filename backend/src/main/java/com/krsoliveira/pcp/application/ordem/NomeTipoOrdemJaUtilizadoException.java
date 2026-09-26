package com.krsoliveira.pcp.application.ordem;

public class NomeTipoOrdemJaUtilizadoException extends RuntimeException {

    public NomeTipoOrdemJaUtilizadoException(String nome) {
        super("Já existe um tipo de ordem com o nome '%s'.".formatted(nome));
    }
}
