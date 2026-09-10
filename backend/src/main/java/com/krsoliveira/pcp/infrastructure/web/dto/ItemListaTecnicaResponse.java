package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemListaTecnicaResponse(
        UUID id,
        UUID materialComponenteId,
        BigDecimal quantidadePlanejada,
        String unidadeDeMedida
) {
    public static ItemListaTecnicaResponse de(ItemListaTecnica item) {
        return new ItemListaTecnicaResponse(
                item.getId(),
                item.getMaterialComponenteId(),
                item.getQuantidadePlanejada(),
                item.getUnidadeDeMedida()
        );
    }
}
