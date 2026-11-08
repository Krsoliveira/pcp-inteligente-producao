package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import com.krsoliveira.pcp.domain.material.Material;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record ListaTecnicaResponse(
        UUID id,
        UUID listaId,
        UUID materialId,
        String versao,
        BigDecimal quantidadeBase,
        StatusListaTecnica status,
        List<ItemListaTecnicaResponse> itens,
        Instant criadaEm,
        Instant atualizadaEm,
        String criadaPor,
        String atualizadaPor,
        // Código e descrição do material produzido: quem vê listas técnicas não precisa consultar o catálogo.
        String materialCodigo,
        String materialDescricao,
        // Número legível da lista (LT-0001), igual em todas as versões.
        String numeroLista
) {
    /** Os ids de material (o produzido e os componentes) que a resposta destas listas precisa nomear. */
    public static Set<UUID> materiaisCitados(Collection<ListaTecnica> listas) {
        Set<UUID> ids = new HashSet<>();
        for (ListaTecnica lista : listas) {
            ids.add(lista.getMaterialId());
            lista.getItens().forEach(item -> ids.add(item.getMaterialComponenteId()));
        }
        return ids;
    }

    public static ListaTecnicaResponse de(ListaTecnica lista, Map<UUID, Material> materiais) {
        Material material = materiais.get(lista.getMaterialId());
        return new ListaTecnicaResponse(
                lista.getId(),
                lista.getListaId(),
                lista.getMaterialId(),
                lista.getVersao(),
                lista.getQuantidadeBase(),
                lista.getStatus(),
                lista.getItens().stream().map(item -> ItemListaTecnicaResponse.de(item, materiais)).toList(),
                lista.getCriadaEm(),
                lista.getAtualizadaEm(),
                lista.getAssinatura().criadoPor(),
                lista.getAssinatura().alteradoPor(),
                material == null ? null : material.getCodigo(),
                material == null ? null : material.getDescricao(),
                lista.getNumeroFormatado()
        );
    }
}
