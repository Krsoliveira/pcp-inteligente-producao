package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Lista Técnica (Bill of Materials — BOM): define quais componentes e em quais
 * quantidades são necessários para produzir uma unidade de um material.
 *
 * Cada material tem UMA lista ({@code listaId}) com várias versões (campo {@code versao}).
 * Várias versões podem estar ATIVAS ao mesmo tempo (receitas alternativas); quem decide
 * quando uma versão sai de uso é o usuário (ADR-0019).
 *
 * Suporta BOM multinível: um componente pode ser um semiacabado que possui
 * sua própria lista técnica.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class ListaTecnica {

    private static final BigDecimal LIMITE_DA_QUANTIDADE_BASE = new BigDecimal("100000000");

    private final UUID id;
    private final UUID listaId;
    private final UUID materialId;
    private final String versao;
    private final BigDecimal quantidadeBase;
    private StatusListaTecnica status;
    private final List<ItemListaTecnica> itens;
    private Assinatura assinatura;

    private ListaTecnica(UUID id, UUID listaId, UUID materialId, String versao, BigDecimal quantidadeBase,
                         StatusListaTecnica status, List<ItemListaTecnica> itens, Assinatura assinatura) {
        this.id = id;
        this.listaId = listaId;
        this.materialId = materialId;
        this.versao = versao;
        this.quantidadeBase = quantidadeBase;
        this.status = status;
        this.itens = new ArrayList<>(itens);
        this.assinatura = assinatura;
    }

    /**
     * Fábrica da PRIMEIRA versão de um material: abre uma lista nova (novo {@code listaId}).
     * Nasce sempre com status EM_REVISAO. A validação de que o material não é MATERIA_PRIMA
     * é responsabilidade do use case, que tem acesso ao repositório de materiais.
     */
    public static ListaTecnica criar(UUID materialId, String versao,
                                     List<ItemListaTecnica> itens, String usuario) {
        return criar(materialId, versao, BigDecimal.ONE, itens, usuario);
    }

    /** Primeira versão de um material, dizendo para quantas unidades (ou kg) do material valem os componentes. */
    public static ListaTecnica criar(UUID materialId, String versao, BigDecimal quantidadeBase,
                                     List<ItemListaTecnica> itens, String usuario) {
        return criarVersao(materialId, UUID.randomUUID(), versao, quantidadeBase, itens, usuario);
    }

    /**
     * Fábrica de uma versão (EM_REVISAO) de uma lista que já existe: reaproveita o
     * {@code listaId} das demais versões do mesmo material.
     */
    public static ListaTecnica criarVersao(UUID materialId, UUID listaId, String versao,
                                           List<ItemListaTecnica> itens, String usuario) {
        return criarVersao(materialId, listaId, versao, BigDecimal.ONE, itens, usuario);
    }

    /** Versão de uma lista que já existe, com a quantidade base (ADR-0027). */
    public static ListaTecnica criarVersao(UUID materialId, UUID listaId, String versao, BigDecimal quantidadeBase,
                                           List<ItemListaTecnica> itens, String usuario) {
        validarQuantidadeBase(quantidadeBase);
        if (listaId == null) {
            throw new RegraDeNegocioException("A lista técnica da versão é obrigatória.");
        }
        if (materialId == null) {
            throw new RegraDeNegocioException("O material da lista técnica é obrigatório.");
        }
        if (versao == null || versao.isBlank()) {
            throw new RegraDeNegocioException("A versão da lista técnica é obrigatória.");
        }
        if (itens == null || itens.isEmpty()) {
            throw new RegraDeNegocioException(
                    "A lista técnica deve ter ao menos um componente.");
        }
        return new ListaTecnica(UUID.randomUUID(), listaId, materialId, versao.trim(), quantidadeBase,
                StatusListaTecnica.EM_REVISAO, itens, Assinatura.nova(usuario));
    }

    /**
     * Reconstrói uma lista EXISTENTE a partir do banco de dados.
     */
    public static ListaTecnica reconstituir(UUID id, UUID listaId, UUID materialId, String versao,
                                            StatusListaTecnica status,
                                            List<ItemListaTecnica> itens,
                                            Assinatura assinatura) {
        return reconstituir(id, listaId, materialId, versao, BigDecimal.ONE, status, itens, assinatura);
    }

    /** Reconstrói uma lista existente com a quantidade base gravada. */
    public static ListaTecnica reconstituir(UUID id, UUID listaId, UUID materialId, String versao,
                                            BigDecimal quantidadeBase, StatusListaTecnica status,
                                            List<ItemListaTecnica> itens, Assinatura assinatura) {
        return new ListaTecnica(id, listaId, materialId, versao, quantidadeBase, status, itens, assinatura);
    }

    private static void validarQuantidadeBase(BigDecimal quantidadeBase) {
        if (quantidadeBase == null || quantidadeBase.signum() <= 0) {
            throw new RegraDeNegocioException("A quantidade base da lista técnica deve ser maior que zero.");
        }
        if (quantidadeBase.stripTrailingZeros().scale() > 4) {
            throw new RegraDeNegocioException("A quantidade base aceita no máximo 4 casas decimais.");
        }
        if (quantidadeBase.compareTo(LIMITE_DA_QUANTIDADE_BASE) >= 0) {
            throw new RegraDeNegocioException("A quantidade base deve ser menor que 100.000.000.");
        }
    }

    /**
     * Quanto de um componente a ordem precisa: quantidade do item ÷ quantidade base × quantidade da ordem, com 4 casas
     * (como o banco). Com a base 1 é a conta de sempre, "por unidade".
     */
    public BigDecimal quantidadeNecessaria(ItemListaTecnica item, int quantidadeDaOrdem) {
        return item.getQuantidadePlanejada()
                .multiply(BigDecimal.valueOf(quantidadeDaOrdem))
                .divide(quantidadeBase, 4, RoundingMode.HALF_UP);
    }

    /**
     * Ativa esta versão. Só é possível se estiver EM_REVISAO. Não afeta as outras versões
     * do material: várias podem estar ATIVAS ao mesmo tempo.
     */
    public void ativar(String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (!status.podeSerAtivada()) {
            throw new RegraDeNegocioException(
                    "Apenas listas em revisão podem ser ativadas. Status atual: %s.".formatted(status));
        }
        this.status = StatusListaTecnica.ATIVA;
        this.assinatura = alterada;
    }

    /**
     * Retira esta versão de uso (ATIVA → OBSOLETA). Ação explícita do usuário.
     */
    public void obsoleter(String usuario) {
        Assinatura alterada = assinatura.alterada(usuario);
        if (!status.podeSerObsoletada()) {
            throw new RegraDeNegocioException(
                    "Apenas listas ativas podem ser obsoletadas. Status atual: %s.".formatted(status));
        }
        this.status = StatusListaTecnica.OBSOLETA;
        this.assinatura = alterada;
    }

    public UUID getId() { return id; }
    public UUID getListaId() { return listaId; }
    public UUID getMaterialId() { return materialId; }
    public String getVersao() { return versao; }
    /** Para quantas unidades (ou kg) do material produzido valem as quantidades dos componentes. */
    public BigDecimal getQuantidadeBase() { return quantidadeBase; }
    public StatusListaTecnica getStatus() { return status; }
    public List<ItemListaTecnica> getItens() { return Collections.unmodifiableList(itens); }
    public Assinatura getAssinatura() { return assinatura; }
    public Instant getCriadaEm() { return assinatura.criadoEm(); }
    public Instant getAtualizadaEm() { return assinatura.alteradoEm(); }
}