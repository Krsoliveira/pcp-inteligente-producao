package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Corpo da requisição de registro de consumo real de material.
 * Se houver desvio (consumida ≠ planejada), a justificativa é obrigatória —
 * validação feita no domínio.
 */
public record RegistrarConsumoRequest(

        @NotNull(message = "quantidadeConsumida é obrigatória")
        @DecimalMin(value = "0.0", message = "quantidadeConsumida não pode ser negativa")
        BigDecimal quantidadeConsumida,

        String justificativa) {

    /** O responsável pela justificativa é o usuário logado — não é informado no corpo. */
    public RegistrarConsumoMaterial.Comando paraComando(UUID consumoMaterialId) {
        return new RegistrarConsumoMaterial.Comando(
                consumoMaterialId, quantidadeConsumida, justificativa);
    }
}
