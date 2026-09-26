package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.util.UUID;

/**
 * Caso de uso: avançar ou cancelar o ciclo de vida de uma ordem.
 *
 * A validação da transição fica no DOMÍNIO ({@code OrdemProducao.alterarStatusPara});
 * aqui apenas carregamos, delegamos e persistimos.
 */
public class AtualizarStatusOrdemProducao {

    private final OrdemProducaoRepository repositorio;

    public AtualizarStatusOrdemProducao(OrdemProducaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    public OrdemProducao executar(UUID id, StatusOrdemProducao novoStatus) {
        if (novoStatus == StatusOrdemProducao.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Para concluir uma ordem utilize o endpoint POST /ordens-producao/{id}/concluir, " +
                    "que valida os consumos de material e gera o lote de produção.");
        }
        OrdemProducao ordem = repositorio.buscarPorId(id)
                .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(id));
        ordem.alterarStatusPara(novoStatus);
        return repositorio.salvar(ordem);
    }
}
