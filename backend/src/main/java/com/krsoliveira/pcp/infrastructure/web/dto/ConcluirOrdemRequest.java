package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.ordem.ConcluirOrdemProducao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo da requisição de conclusão de ordem de produção.
 * Ao concluir, o sistema valida os consumos e gera um lote rastreável.
 */
public record ConcluirOrdemRequest(

        @NotNull(message = "quantidadeProduzida é obrigatória")
        @Positive(message = "quantidadeProduzida deve ser maior que zero")
        BigDecimal quantidadeProduzida,

        @NotNull(message = "dataFabricacao é obrigatória")
        LocalDate dataFabricacao,

        @NotNull(message = "dataValidade é obrigatória")
        LocalDate dataValidade) {

    public ConcluirOrdemProducao.Comando paraComando(UUID ordemId) {
        return new ConcluirOrdemProducao.Comando(
                ordemId, quantidadeProduzida, dataFabricacao, dataValidade);
    }
}
