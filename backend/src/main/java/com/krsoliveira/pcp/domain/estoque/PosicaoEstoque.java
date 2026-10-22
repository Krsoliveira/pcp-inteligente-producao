package com.krsoliveira.pcp.domain.estoque;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Posição de estoque de um material, calculada a partir dos saldos dos lotes (ADR-0012):
 * não há tabela de estoque separada que possa divergir dos lotes.
 *
 * @param saldoDisponivel   soma dos saldos dos lotes DISPONIVEIS e dentro da validade
 * @param lotesDisponiveis  quantos lotes compõem o saldo disponível
 * @param saldoIndisponivel saldo parado em lotes bloqueados ou vencidos
 * @param proximoVencimento validade mais próxima entre os lotes disponíveis
 * @param ultimaEntrada     quando o último lote do material entrou (compra ou produção)
 */
public record PosicaoEstoque(UUID materialId,
                             BigDecimal saldoDisponivel,
                             int lotesDisponiveis,
                             BigDecimal saldoIndisponivel,
                             LocalDate proximoVencimento,
                             Instant ultimaEntrada) {

    public static PosicaoEstoque vazia(UUID materialId) {
        return new PosicaoEstoque(materialId, BigDecimal.ZERO, 0, BigDecimal.ZERO, null, null);
    }
}
