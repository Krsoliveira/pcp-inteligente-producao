package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Lista Técnica (Bill of Materials — BOM): define quais componentes e em quais
 * quantidades são necessários para produzir uma unidade de um material.
 *
 * Cada material pode ter múltiplas versões de lista (campo {@code versao}),
 * mas apenas UMA pode estar ATIVA por vez — regra garantida pelo use case
 * {@code AtivarListaTecnica}.
 *
 * Suporta BOM multinível: um componente pode ser um semiacabado que possui
 * sua própria lista técnica.
 *
 * Classe de domínio PURA: sem anotações de framework.
 */
public class ListaTecnica {

    private final UUID id;
    private final UUID materialId;
    private final String versao;
    private StatusListaTecnica status;
    private final List<ItemListaTecnica> itens;
    private final Instant criadaEm;
    private Instant atualizadaEm;

    private ListaTecnica(UUID id, UUID materialId, String versao, StatusListaTecnica status,
                         List<ItemListaTecnica> itens, Instant criadaEm, Instant atualizadaEm) {
        this.id = id;
        this.materialId = materialId;
        this.versao = versao;
        this.status = status;
        this.itens = new ArrayList<>(itens);
        this.criadaEm = criadaEm;
        this.atualizadaEm = atualizadaEm;
    }

    /**
     * Fábrica para uma lista NOVA. Nasce sempre com status EM_REVISAO.
     * A validação de que o material não é MATERIA_PRIMA é responsabilidade
     * do use case, que tem acesso ao repositório de materiais.
     */
    public static ListaTecnica criar(UUID materialId, String versao,
                                     List<ItemListaTecnica> itens) {
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
        Instant agora = Instant.now();
        return new ListaTecnica(UUID.randomUUID(), materialId, versao.trim(),
                StatusListaTecnica.EM_REVISAO, itens, agora, agora);
    }

    /**
     * Reconstrói uma lista EXISTENTE a partir do banco de dados.
     */
    public static ListaTecnica reconstituir(UUID id, UUID materialId, String versao,
                                            StatusListaTecnica status,
                                            List<ItemListaTecnica> itens,
                                            Instant criadaEm, Instant atualizadaEm) {
        return new ListaTecnica(id, materialId, versao, status, itens, criadaEm, atualizadaEm);
    }

    /**
     * Ativa esta lista. Só é possível se estiver EM_REVISAO.
     * Cabe ao use case garantir que a lista atualmente ATIVA seja obsoletada antes.
     */
    public void ativar() {
        if (!status.podeSerAtivada()) {
            throw new RegraDeNegocioException(
                    "Apenas listas em revisão podem ser ativadas. Status atual: %s.".formatted(status));
        }
        this.status = StatusListaTecnica.ATIVA;
        this.atualizadaEm = Instant.now();
    }

    /**
     * Marca esta lista como OBSOLETA. Chamado pelo use case ao ativar uma versão mais nova.
     */
    public void obsoleter() {
        if (!status.podeSerObsoletada()) {
            throw new RegraDeNegocioException(
                    "Apenas listas ativas podem ser obsoletadas. Status atual: %s.".formatted(status));
        }
        this.status = StatusListaTecnica.OBSOLETA;
        this.atualizadaEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getMaterialId() { return materialId; }
    public String getVersao() { return versao; }
    public StatusListaTecnica getStatus() { return status; }
    public List<ItemListaTecnica> getItens() { return Collections.unmodifiableList(itens); }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}