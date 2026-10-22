package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.util.UUID;

/**
 * Caso de uso: avançar (ou cancelar) o status de uma ordem. Cada transição fica na
 * trilha de auditoria com o status de origem, o de destino e quem a fez.
 */
public class AtualizarStatusOrdemProducao {

    private final OrdemProducaoRepository repositorio;
    private final ExecucaoAuditada execucao;

    public AtualizarStatusOrdemProducao(OrdemProducaoRepository repositorio, ExecucaoAuditada execucao) {
        this.repositorio = repositorio;
        this.execucao = execucao;
    }

    public OrdemProducao executar(UUID id, StatusOrdemProducao novoStatus) {
        if (novoStatus == StatusOrdemProducao.CONCLUIDA) {
            throw new RegraDeNegocioException(
                    "Para concluir uma ordem utilize o endpoint POST /ordens-producao/{id}/concluir, " +
                    "que valida os consumos de material e gera o lote de produção.");
        }
        return execucao.executar(ctx -> {
            OrdemProducao ordem = repositorio.buscarPorId(id)
                    .orElseThrow(() -> new OrdemProducaoNaoEncontradaException(id));
            StatusOrdemProducao anterior = ordem.getStatus();
            ordem.alterarStatusPara(novoStatus, ctx.usuario());
            OrdemProducao salva = repositorio.salvar(ordem);
            ctx.registrar(TipoEntidade.ORDEM_PRODUCAO, salva.getId(), salva.getCodigo(),
                    AcaoAuditoria.STATUS_ALTERADO,
                    Detalhes.vazio().mudanca("status", anterior, novoStatus));
            return salva;
        });
    }
}
