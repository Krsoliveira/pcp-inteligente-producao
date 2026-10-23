package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.material.MaterialNaoEncontradoException;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.PoliticaDeComponentes;
import com.krsoliveira.pcp.domain.lista.ListaTecnicaRepository;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.MaterialRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: cadastrar uma nova versão de lista técnica (BOM) para um material.
 * A lista nasce EM_REVISAO; a ativação é feita por {@link AtivarListaTecnica}.
 */
public class CadastrarListaTecnica {

    private final ListaTecnicaRepository listaTecnicaRepository;
    private final MaterialRepository materialRepository;
    private final ExecucaoAuditada execucao;

    public CadastrarListaTecnica(ListaTecnicaRepository listaTecnicaRepository,
                                 MaterialRepository materialRepository,
                                 ExecucaoAuditada execucao) {
        this.listaTecnicaRepository = listaTecnicaRepository;
        this.materialRepository = materialRepository;
        this.execucao = execucao;
    }

    public record ItemComando(UUID materialComponenteId, BigDecimal quantidadePlanejada,
                              String unidadeDeMedida) {}

    /** @param quantidadeBase para quantas unidades (ou kg) do material valem os componentes; {@code null} é 1 */
    public record Comando(UUID materialId, String versao, BigDecimal quantidadeBase, List<ItemComando> itens) {

        public Comando(UUID materialId, String versao, List<ItemComando> itens) {
            this(materialId, versao, null, itens);
        }
    }

    public UUID executar(Comando comando) {
        return execucao.executar(ctx -> {
            Material material = materialRepository.buscarPorId(comando.materialId())
                    .orElseThrow(() -> new MaterialNaoEncontradoException(comando.materialId()));

            if (!material.getTipo().podeTermListaTecnica()) {
                throw new RegraDeNegocioException(
                        "Matéria-prima não pode ter lista técnica. Material: %s (%s)."
                                .formatted(material.getCodigo(), material.getTipo()));
            }

            // Regra inegociável (ADR-0024): produto acabado só é componente de lista de semiacabado.
            PoliticaDeComponentes.validarLista(material, comando.itens().stream()
                    .map(i -> materialRepository.buscarPorId(i.materialComponenteId())
                            .orElseThrow(() -> new MaterialNaoEncontradoException(i.materialComponenteId())))
                    .toList());

            if (listaTecnicaRepository.existeVersaoParaMaterial(
                    comando.materialId(), comando.versao())) {
                throw new VersaoListaTecnicaJaExisteException(comando.materialId(), comando.versao());
            }

            List<ItemListaTecnica> itens = comando.itens().stream()
                    .map(i -> ItemListaTecnica.criar(
                            i.materialComponenteId(),
                            i.quantidadePlanejada(),
                            i.unidadeDeMedida()))
                    .toList();

            BigDecimal base = comando.quantidadeBase() == null ? BigDecimal.ONE : comando.quantidadeBase();

            // Cada material tem uma lista; a nova versão entra nela (ou abre a lista, se for a primeira).
            ListaTecnica lista = listaTecnicaRepository.listarPorMaterial(comando.materialId()).stream()
                    .findFirst()
                    .map(existente -> ListaTecnica.criarVersao(comando.materialId(), existente.getListaId(),
                            comando.versao(), base, itens, ctx.usuario()))
                    .orElseGet(() -> ListaTecnica.criar(comando.materialId(), comando.versao(), base, itens,
                            ctx.usuario()));
            listaTecnicaRepository.salvar(lista);

            ctx.registrar(TipoEntidade.LISTA_TECNICA, lista.getId(),
                    material.getCodigo() + " " + lista.getVersao(), AcaoAuditoria.CRIADO,
                    Detalhes.com("material", material.getCodigo())
                            .e("listaId", lista.getListaId())
                            .e("versao", lista.getVersao())
                            .e("status", lista.getStatus())
                            .e("componentes", itens.size())
                            .e("quantidadeBase", base));
            return lista.getId();
        });
    }
}
