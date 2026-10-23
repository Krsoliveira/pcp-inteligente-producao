package com.krsoliveira.pcp.infrastructure.web.dto;

import com.krsoliveira.pcp.application.relatorio.LinhaDeConsumoDeOrdem;
import com.krsoliveira.pcp.application.relatorio.RelatorioConsumoDeOrdens;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Relatório de consumo de ordens concluídas: uma linha por ordem e componente. */
public record ConsumoDeOrdensResponse(LocalDate de, LocalDate ate, long totalDeOrdens, List<Linha> linhas) {

    public record Linha(UUID ordemId, String ordemCodigo, LocalDate fimPlanejado,
                        String produtoCodigo, String produtoDescricao,
                        String componenteCodigo, String componenteDescricao, String unidadeDeMedida,
                        BigDecimal quantidadePlanejada, BigDecimal quantidadeConsumida, BigDecimal desvio) {

        static Linha de(LinhaDeConsumoDeOrdem l) {
            return new Linha(l.ordemId(), l.ordemCodigo(), l.fimPlanejado(), l.produtoCodigo(), l.produtoDescricao(),
                    l.componenteCodigo(), l.componenteDescricao(), l.unidadeDeMedida(),
                    l.quantidadePlanejada(), l.quantidadeConsumida(), l.desvio());
        }
    }

    public static ConsumoDeOrdensResponse de(RelatorioConsumoDeOrdens relatorio) {
        return new ConsumoDeOrdensResponse(relatorio.de(), relatorio.ate(), relatorio.totalDeOrdens(),
                relatorio.linhas().stream().map(Linha::de).toList());
    }
}
