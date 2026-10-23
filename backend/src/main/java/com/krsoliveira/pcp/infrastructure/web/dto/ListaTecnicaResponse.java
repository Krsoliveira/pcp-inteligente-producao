package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        String atualizadaPor
) {
    public static ListaTecnicaResponse de(ListaTecnica lista) {
        return new ListaTecnicaResponse(
                lista.getId(),
                lista.getListaId(),
                lista.getMaterialId(),
                lista.getVersao(),
                lista.getQuantidadeBase(),
                lista.getStatus(),
                lista.getItens().stream().map(ItemListaTecnicaResponse::de).toList(),
                lista.getCriadaEm(),
                lista.getAtualizadaEm(),
                lista.getAssinatura().criadoPor(),
                lista.getAssinatura().alteradoPor()
        );
    }
}
