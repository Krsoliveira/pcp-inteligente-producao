package com.krsoliveira.pcp.application.lista;

import java.util.UUID;

public class VersaoListaTecnicaJaExisteException extends RuntimeException {
    public VersaoListaTecnicaJaExisteException(UUID materialId, String versao) {
        super("Já existe uma lista técnica com versão '%s' para o material %s."
                .formatted(versao, materialId));
    }
}