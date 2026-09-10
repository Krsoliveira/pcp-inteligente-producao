package com.krsoliveira.pcp.application.material;

public class CodigoMaterialJaUtilizadoException extends RuntimeException {
    public CodigoMaterialJaUtilizadoException(String codigo) {
        super("Já existe um material com o código '%s'.".formatted(codigo));
    }
}