package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;

import com.krsoliveira.pcp.domain.material.Material;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ItemListaTecnicaResponse(
        UUID id,
        UUID materialComponenteId,
        BigDecimal quantidadePlanejada,
        String unidadeDeMedida,
        // Código e descrição do componente: quem vê listas técnicas não precisa consultar o catálogo de materiais.
        String componenteCodigo,
        String componenteDescricao
) {
    public static ItemListaTecnicaResponse de(ItemListaTecnica item, Map<UUID, Material> materiais) {
        Material componente = materiais.get(item.getMaterialComponenteId());
        return new ItemListaTecnicaResponse(
                item.getId(),
                item.getMaterialComponenteId(),
                item.getQuantidadePlanejada(),
                item.getUnidadeDeMedida(),
                componente == null ? null : componente.getCodigo(),
                componente == null ? null : componente.getDescricao()
        );
    }
}
