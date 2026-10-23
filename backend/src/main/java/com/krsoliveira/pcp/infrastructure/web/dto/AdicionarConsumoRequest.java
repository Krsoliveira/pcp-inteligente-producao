package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.consumo.AdicionarConsumoMaterial;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Item adicionado ao consumo de uma ordem em produção, fora da lista técnica (ADR-0025). A linha nasce com
 * planejado zero e já é registrada: a justificativa é obrigatória e {@code alocacoes} diz de quais lotes saiu
 * (a soma é igual à quantidade consumida).
 */
public record AdicionarConsumoRequest(

        @NotNull(message = "materialId é obrigatório")
        UUID materialId,

        @NotNull(message = "quantidadeConsumida é obrigatória")
        @DecimalMin(value = "0.0", inclusive = false, message = "quantidadeConsumida deve ser maior que zero")
        BigDecimal quantidadeConsumida,

        String justificativa,

        @Valid
        List<RegistrarConsumoRequest.AlocacaoRequest> alocacoes) {

    public AdicionarConsumoMaterial.Comando paraComando(UUID ordemId) {
        List<RegistrarConsumoMaterial.Alocacao> lotes = alocacoes == null ? List.of()
                : alocacoes.stream()
                        .map(a -> new RegistrarConsumoMaterial.Alocacao(a.loteId(), a.quantidade()))
                        .toList();
        return new AdicionarConsumoMaterial.Comando(ordemId, materialId, quantidadeConsumida, justificativa, lotes);
    }
}
