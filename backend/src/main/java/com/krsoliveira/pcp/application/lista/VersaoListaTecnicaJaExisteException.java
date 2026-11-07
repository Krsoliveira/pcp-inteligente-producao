package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.ConflitoException;
import java.util.UUID;

public class VersaoListaTecnicaJaExisteException extends ConflitoException {
    public VersaoListaTecnicaJaExisteException(UUID materialId, String versao) {
        super("Já existe uma lista técnica com versão '%s' para o material %s."
                .formatted(versao, materialId));
    }
}