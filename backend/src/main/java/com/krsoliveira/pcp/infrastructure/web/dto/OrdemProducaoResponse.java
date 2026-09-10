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
 *
 * Fase 5a (ADR-0007): produto (String) substituído por materialId + listaTecnicaId (UUIDs).
 */
public record OrdemProducaoResponse(UUID id,
                                    String codigo,
                                    UUID materialId,
                                    UUID listaTecnicaId,
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
                ordem.getMaterialId(),
                ordem.getListaTecnicaId(),
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
