package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de uma ordem devolvida pela API.
 *
 * O campo {@code atrasada} é CALCULADO no momento da resposta (regra do domínio
 * aplicada à data de hoje) — não existe coluna "atrasada" no banco.
 */
public record OrdemProducaoResponse(UUID id,
                                    String codigo,
                                    String produto,
                                    String centroDeTrabalho,
                                    int quantidade,
                                    LocalDate inicioPlanejado,
                                    LocalDate fimPlanejado,
                                    StatusOrdemProducao status,
                                    boolean atrasada,
                                    Instant criadaEm,
                                    Instant atualizadaEm) {

    public static OrdemProducaoResponse de(OrdemProducao ordem) {
        return new OrdemProducaoResponse(
                ordem.getId(),
                ordem.getCodigo(),
                ordem.getProduto(),
                ordem.getCentroDeTrabalho(),
                ordem.getQuantidade(),
                ordem.getInicioPlanejado(),
                ordem.getFimPlanejado(),
                ordem.getStatus(),
                ordem.estaAtrasada(LocalDate.now()),
                ordem.getCriadaEm(),
                ordem.getAtualizadaEm());
    }
}
