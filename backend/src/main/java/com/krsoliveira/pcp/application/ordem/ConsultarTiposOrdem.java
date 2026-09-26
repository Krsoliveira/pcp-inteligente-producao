package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultar tipos de ordem cadastrados.
 */
public class ConsultarTiposOrdem {

    private final TipoOrdemRepository tipoOrdemRepository;

    public ConsultarTiposOrdem(TipoOrdemRepository tipoOrdemRepository) {
        this.tipoOrdemRepository = tipoOrdemRepository;
    }

    public List<TipoOrdem> listarTodos() {
        return tipoOrdemRepository.listarTodos();
    }

    public TipoOrdem porId(UUID id) {
        return tipoOrdemRepository.buscarPorId(id)
                .orElseThrow(() -> new TipoOrdemNaoEncontradoException(id));
    }
}
