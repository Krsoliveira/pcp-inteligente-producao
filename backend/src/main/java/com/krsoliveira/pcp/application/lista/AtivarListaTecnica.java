package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ativar uma versão de lista técnica. A versão ativa anterior do mesmo
 * material passa a OBSOLETA — as duas mudanças são atômicas e auditadas.
 */
public class AtivarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public AtivarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
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

            Optional<ListaTecnica> ativaAtual =
                    listaTecnicaRepository.buscarAtivaParaMaterial(lista.getMaterialId());

            ativaAtual.ifPresent(ativa -> {
                ativa.obsoleter(ctx.usuario());
                listaTecnicaRepository.salvar(ativa);
                ctx.registrar(TipoEntidade.LISTA_TECNICA, ativa.getId(),
                        codigoMaterial + " " + ativa.getVersao(), AcaoAuditoria.OBSOLETADA,
                        Detalhes.com("versao", ativa.getVersao())
                                .e("substituidaPor", lista.getVersao()));
            });

            lista.ativar(ctx.usuario());
            listaTecnicaRepository.salvar(lista);
            ctx.registrar(TipoEntidade.LISTA_TECNICA, lista.getId(),
                    codigoMaterial + " " + lista.getVersao(), AcaoAuditoria.ATIVADA,
                    Detalhes.com("versao", lista.getVersao())
                            .e("versaoAnterior", ativaAtual.map(ListaTecnica::getVersao).orElse(null)));
            return null;
        });
    }
}
