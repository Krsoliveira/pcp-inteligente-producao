package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Corpo da requisição de registro de consumo real de material.
 * Se houver desvio (consumida ≠ planejada), justificativa e justificadoPor
 * são obrigatórios — validação feita no domínio.
 */
public record RegistrarConsumoRequest(

        @NotNull(message = "quantidadeConsumida é obrigatória")
        @DecimalMin(value = "0.0", message = "quantidadeConsumida não pode ser negativa")
        BigDecimal quantidadeConsumida,

        String justificativa,

        String justificadoPor) {

    public RegistrarConsumoMaterial.Comando paraComando(UUID consumoMaterialId) {
        return new RegistrarConsumoMaterial.Comando(
                consumoMaterialId, quantidadeConsumida, justificativa, justificadoPor);
    }
}
