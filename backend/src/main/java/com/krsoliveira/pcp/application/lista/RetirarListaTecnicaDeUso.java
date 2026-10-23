package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.util.UUID;

/**
 * Caso de uso: retirar de uso uma versão ativa de lista técnica (ATIVA → OBSOLETA).
 * É uma decisão do usuário (ADR-0019): ativar outra versão não retira nenhuma.
 */
public class RetirarListaTecnicaDeUso {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public RetirarListaTecnicaDeUso(ListaTecnicaRepository listaTecnicaRepository,
                                    MaterialRepository materialRepository,
                                    ExecucaoAuditada execucao) {
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public void executar(UUID listaTecnicaId) {
        execucao.executar(ctx -> {
            ListaTecnica lista = listaTecnicaRepository.buscarPorId(listaTecnicaId)
                    .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(listaTecnicaId));
            String codigoMaterial = materialRepository.buscarPorId(lista.getMaterialId())
                    .map(Material::getCodigo).orElse("?");

            lista.obsoleter(ctx.usuario());
            listaTecnicaRepository.salvar(lista);
            ctx.registrar(TipoEntidade.LISTA_TECNICA, lista.getId(),
                    codigoMaterial + " " + lista.getVersao(), AcaoAuditoria.OBSOLETADA,
                    Detalhes.com("versao", lista.getVersao()));
            return null;
        });
    }
}
