package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListaTecnicaResponse(
        UUID id,
        UUID materialId,
        String versao,
        StatusListaTecnica status,
        List<ItemListaTecnicaResponse> itens,
        Instant criadaEm,
        Instant atualizadaEm
) {
    public static ListaTecnicaResponse de(ListaTecnica lista) {
        return new ListaTecnicaResponse(
                lista.getId(),
                lista.getMaterialId(),
                lista.getVersao(),
                lista.getStatus(),
                lista.getItens().stream().map(ItemListaTecnicaResponse::de).toList(),
                lista.getCriadaEm(),
                lista.getAtualizadaEm()
        );
    }
}
