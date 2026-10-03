package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;
import jakarta.validation.Valid;

/**
 * Corpo da requisição de registro de consumo real de material.
 * Se houver desvio (consumida ≠ planejada), a justificativa é obrigatória. As
 * {@code alocacoes} dizem de quais lotes saiu o material: a soma deve ser igual à
 * quantidade consumida (consumo zero não aloca) — validações no caso de uso e no domínio.
 */
public record RegistrarConsumoRequest(

        @NotNull(message = "quantidadeConsumida é obrigatória")
        @DecimalMin(value = "0.0", message = "quantidadeConsumida não pode ser negativa")
        BigDecimal quantidadeConsumida,

        String justificativa,

        @Valid
        List<AlocacaoRequest> alocacoes) {

    public record AlocacaoRequest(
            @NotNull(message = "loteId é obrigatório")
            UUID loteId,

            @NotNull(message = "quantidade é obrigatória")
            @DecimalMin(value = "0.0", inclusive = false, message = "quantidade deve ser maior que zero")
            BigDecimal quantidade) {}

    /** O responsável pela justificativa é o usuário logado — não é informado no corpo. */
    public RegistrarConsumoMaterial.Comando paraComando(UUID consumoMaterialId) {
        List<RegistrarConsumoMaterial.Alocacao> lotes = alocacoes == null ? List.of()
                : alocacoes.stream()
                        .map(a -> new RegistrarConsumoMaterial.Alocacao(a.loteId(), a.quantidade()))
                        .toList();
        return new RegistrarConsumoMaterial.Comando(
                consumoMaterialId, quantidadeConsumida, justificativa, lotes);
    }
}
