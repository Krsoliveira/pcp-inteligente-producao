package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: consultas de ordens de produção (por id e listagem).
 */
public class ConsultarOrdensProducao {

    private final OrdemProducaoRepository repositorio;

    public ConsultarOrdensProducao(OrdemProducaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public OrdemProducao porId(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(id));
    }

    public List<OrdemProducao> listar() {
        return repositorio.listarTodas();
    }
}
