package com.krsoliveira.pcp.application.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Consumo total de um componente numa ordem concluída (a soma dos lotes de onde saiu, já consolidada no consumo).
 *
 * @param desvio consumida − planejada; {@code null} se o consumo não tem quantidade consumida
 */
public record LinhaDeConsumoDeOrdem(UUID ordemId, String ordemCodigo, LocalDate fimPlanejado,
                                    String produtoCodigo, String produtoDescricao,
                                    String componenteCodigo, String componenteDescricao, String unidadeDeMedida,
                                    BigDecimal quantidadePlanejada, BigDecimal quantidadeConsumida, BigDecimal desvio) {
}
