package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.consumo.LancarConsumoDaOrdem;
import com.krsoliveira.pcp.application.consumo.RegistrarConsumoMaterial;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lançamento do consumo de uma ordem em produção, de uma vez e tudo ou nada (ADR-0026): o consumo das linhas da lista
 * técnica e os itens que não estão nela. A lista de itens pode ser vazia.
 */
public record LancarConsumoRequest(

        @Valid @Size(max = 200, message = "No máximo 200 linhas por lançamento")
        List<Linha> consumos,

        @Valid @Size(max = 200, message = "No máximo 200 itens adicionais por lançamento")
        List<Item> itensAdicionais) {

    public LancarConsumoRequest {
        consumos = consumos == null ? List.of() : consumos;
        itensAdicionais = itensAdicionais == null ? List.of() : itensAdicionais;
    }

    public record Linha(
            @NotNull(message = "consumoId é obrigatório")
            UUID consumoId,

            @NotNull(message = "quantidadeConsumida é obrigatória")
            @DecimalMin(value = "0.0", message = "quantidadeConsumida não pode ser negativa")
            BigDecimal quantidadeConsumida,

            String justificativa,

            @Valid
            List<RegistrarConsumoRequest.AlocacaoRequest> alocacoes) {}

    public record Item(
            @NotNull(message = "materialId é obrigatório")
            UUID materialId,

            @NotNull(message = "quantidadeConsumida é obrigatória")
            @DecimalMin(value = "0.0", inclusive = false, message = "quantidadeConsumida deve ser maior que zero")
            BigDecimal quantidadeConsumida,

            String justificativa,

            @Valid
            List<RegistrarConsumoRequest.AlocacaoRequest> alocacoes) {}

    public LancarConsumoDaOrdem.Comando paraComando(UUID ordemId) {
        return new LancarConsumoDaOrdem.Comando(ordemId,
                consumos.stream().map(l -> new LancarConsumoDaOrdem.Linha(l.consumoId(), l.quantidadeConsumida(),
                        l.justificativa(), lotes(l.alocacoes()))).toList(),
                itensAdicionais.stream().map(i -> new LancarConsumoDaOrdem.ItemAdicional(i.materialId(), i.quantidadeConsumida(),
                        i.justificativa(), lotes(i.alocacoes()))).toList());
    }

    private static List<RegistrarConsumoMaterial.Alocacao> lotes(List<RegistrarConsumoRequest.AlocacaoRequest> alocacoes) {
        return alocacoes == null ? List.of()
                : alocacoes.stream().map(a -> new RegistrarConsumoMaterial.Alocacao(a.loteId(), a.quantidade())).toList();
    }
}
