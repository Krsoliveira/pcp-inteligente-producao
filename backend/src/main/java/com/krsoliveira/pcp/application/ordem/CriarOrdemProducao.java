package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: criar uma ordem de produção. A ordem e seus consumos projetados pela
 * lista técnica são gravados juntos (atomicamente), com o usuário responsável.
 */
public class CriarOrdemProducao {

    private final OrdemProducaoRepository repositorio;
    private final ProjetarConsumoMaterial projetarConsumoMaterial;
    private final MaterialRepository materialRepository;
    private final ListaTecnicaRepository listaTecnicaRepository;
    private final ExecucaoAuditada execucao;

    public CriarOrdemProducao(OrdemProducaoRepository repositorio,
                              ProjetarConsumoMaterial projetarConsumoMaterial,
                              MaterialRepository materialRepository,
                              ListaTecnicaRepository listaTecnicaRepository,
                              ExecucaoAuditada execucao) {
        this.repositorio = repositorio;
        this.projetarConsumoMaterial = projetarConsumoMaterial;
        this.materialRepository = materialRepository;
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.execucao = execucao;
    }

    public OrdemProducao executar(Comando comando) {
        return execucao.executar(ctx -> {
            if (repositorio.existePorCodigo(comando.codigo())) {
                throw new CodigoJaUtilizadoException(comando.codigo());
            }
            OrdemProducao ordem = OrdemProducao.criar(
                    comando.codigo(),
                    comando.materialId(),
                    comando.listaTecnicaId(),
                    comando.tipoOrdemId(),
                    comando.centroDeTrabalho(),
                    comando.quantidade(),
                    comando.inicioPlanejado(),
                    comando.fimPlanejado(),
                    ctx.usuario());
            OrdemProducao ordemSalva = repositorio.salvar(ordem);

            List<ConsumoMaterial> consumos = projetarConsumoMaterial.executar(
                    new ProjetarConsumoMaterial.Comando(
                            ordemSalva.getId(),
                            ordemSalva.getListaTecnicaId(),
                            ordemSalva.getQuantidade(),
                            ctx.usuario()));

            ctx.registrar(TipoEntidade.ORDEM_PRODUCAO, ordemSalva.getId(), ordemSalva.getCodigo(),
                    AcaoAuditoria.CRIADO,
                    Detalhes.com("codigo", ordemSalva.getCodigo())
                            .e("material", materialRepository.buscarPorId(ordemSalva.getMaterialId())
                                    .map(Material::getCodigo).orElse(null))
                            .e("versaoListaTecnica", listaTecnicaRepository
                                    .buscarPorId(ordemSalva.getListaTecnicaId())
                                    .map(ListaTecnica::getVersao).orElse(null))
                            .e("centroDeTrabalho", ordemSalva.getCentroDeTrabalho())
                            .e("quantidade", ordemSalva.getQuantidade())
                            .e("inicioPlanejado", ordemSalva.getInicioPlanejado())
                            .e("fimPlanejado", ordemSalva.getFimPlanejado())
                            .e("status", ordemSalva.getStatus())
                            .e("consumosProjetados", consumos.size()));
            return ordemSalva;
        });
    }

    /**
     * Dados de entrada do caso de uso, desacoplados do formato HTTP.
     * @param tipoOrdemId categorização opcional da ordem (pode ser {@code null})
     */
    public record Comando(String codigo,
                          UUID materialId,
                          UUID listaTecnicaId,
                          UUID tipoOrdemId,
                          String centroDeTrabalho,
                          int quantidade,
                          LocalDate inicioPlanejado,
                          LocalDate fimPlanejado) {}
}
