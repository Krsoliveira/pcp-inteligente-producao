package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;

import java.time.Instant;
import java.util.UUID;

public record TipoOrdemResponse(UUID id,
                                String nome,
                                String descricao,
                                String cor,
                                Instant criadoEm,
                                Instant atualizadoEm) {

    public static TipoOrdemResponse de(TipoOrdem tipo) {
        return new TipoOrdemResponse(
                tipo.getId(),
                tipo.getNome(),
                tipo.getDescricao(),
                tipo.getCor(),
                tipo.getCriadoEm(),
                tipo.getAtualizadoEm());
    }
}
