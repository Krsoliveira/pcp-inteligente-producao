package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ConsumoMaterialResponse(UUID id,
                                      UUID ordemProducaoId,
                                      UUID materialId,
                                      BigDecimal quantidadePlanejada,
                                      BigDecimal quantidadeConsumida,
                                      BigDecimal desvio,
                                      String unidadeDeMedida,
                                      boolean registrado,
                                      boolean justificado,
                                      String justificativa,
                                      String justificadoPor,
                                      Instant justificadoEm,
                                      Instant criadoEm,
                                      String criadoPor,
                                      Instant atualizadoEm,
                                      String atualizadoPor) {

    public static ConsumoMaterialResponse de(ConsumoMaterial consumo) {
        return new ConsumoMaterialResponse(
                consumo.getId(),
                consumo.getOrdemProducaoId(),
                consumo.getMaterialId(),
                consumo.getQuantidadePlanejada(),
                consumo.getQuantidadeConsumida(),
                consumo.getDesvio(),
                consumo.getUnidadeDeMedida(),
                consumo.estaRegistrado(),
                consumo.estaJustificado(),
                consumo.getJustificativa(),
                consumo.getJustificadoPor(),
                consumo.getJustificadoEm(),
                consumo.getCriadoEm(),
                consumo.getAssinatura().criadoPor(),
                consumo.getAssinatura().alteradoEm(),
                consumo.getAssinatura().alteradoPor());
    }
}
