package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.consumo.ProjetarConsumoMaterial;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.application.lista.ListaTecnicaNaoEncontradaException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.PoliticaDeComponentes;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.OrdemProducaoRepository;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

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
    private final TipoOrdemRepository tipoOrdemRepository;
    private final ExecucaoAuditada execucao;

    public CriarOrdemProducao(OrdemProducaoRepository repositorio,
                              ProjetarConsumoMaterial projetarConsumoMaterial,
                              MaterialRepository materialRepository,
                              ListaTecnicaRepository listaTecnicaRepository,
                              TipoOrdemRepository tipoOrdemRepository,
                              ExecucaoAuditada execucao) {
        this.repositorio = repositorio;
        this.projetarConsumoMaterial = projetarConsumoMaterial;
        this.materialRepository = materialRepository;
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.execucao = execucao;
    }

    public OrdemProducao executar(Comando comando) {
        return execucao.executar(ctx -> {
            if (repositorio.existePorCodigo(comando.codigo())) {
                throw new CodigoJaUtilizadoException(comando.codigo());
            }
            ListaTecnica lista = validarVersaoDaLista(comando);
            validarConsumoDeProdutoAcabado(comando, lista);
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
                            .e("versaoListaTecnica", lista.getVersao())
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
     * Regra inegociável (ADR-0024): produto acabado só é consumido em ordem do tipo Conversão, que só produz
     * semiacabado.
     */
    private void validarConsumoDeProdutoAcabado(Comando comando, ListaTecnica lista) {
        Material produzido = materialRepository.buscarPorId(comando.materialId())
                .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));
        List<Material> componentes = lista.getItens().stream()
                .map(i -> materialRepository.buscarPorId(i.getMaterialComponenteId())
                        .orElseThrow(() -> new MaterialNaoEncontradoException(i.getMaterialComponenteId())))
                .toList();
        TipoOrdem tipo = comando.tipoOrdemId() == null ? null : tipoOrdemRepository.buscarPorId(comando.tipoOrdemId())
                .orElseThrow(() -> new TipoOrdemNaoEncontradoException(comando.tipoOrdemId()));
        PoliticaDeComponentes.validarOrdem(produzido, componentes, tipo);
    }

    /**
     * A versão escolhida precisa ser do material da ordem e estar em uso (ATIVA). Como várias
     * versões podem estar ativas ao mesmo tempo (ADR-0019), a escolha é explícita e validada aqui.
     */
    private ListaTecnica validarVersaoDaLista(Comando comando) {
        ListaTecnica lista = listaTecnicaRepository.buscarPorId(comando.listaTecnicaId())
                .orElseThrow(() -> new ListaTecnicaNaoEncontradaException(comando.listaTecnicaId()));
        if (!lista.getMaterialId().equals(comando.materialId())) {
            throw new RegraDeNegocioException("A versão da lista técnica escolhida não é do material da ordem.");
        }
        if (lista.getStatus() != StatusListaTecnica.ATIVA) {
            throw new RegraDeNegocioException(
                    "Só versões ativas da lista técnica podem ser usadas em uma ordem. A versão %s está %s."
                            .formatted(lista.getVersao(), lista.getStatus()));
        }
        return lista;
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
