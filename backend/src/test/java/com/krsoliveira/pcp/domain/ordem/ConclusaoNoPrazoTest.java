package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** "Concluída no prazo" usa o instante que o sistema registrou, no fuso informado. */
class ConclusaoNoPrazoTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate FIM = LocalDate.of(2026, 3, 9);

    private static OrdemProducao ordem(StatusOrdemProducao status, Instant alteradaEm) {
        Assinatura assinatura = new Assinatura("ana", Instant.parse("2026-03-01T12:00:00Z"), "ana", alteradaEm);
        return OrdemProducao.reconstituir(UUID.randomUUID(), "OP-1", UUID.randomUUID(), UUID.randomUUID(), null,
                "Usinagem", 10, BigDecimal.TEN, LocalDate.of(2026, 3, 1), FIM, status, assinatura);
    }

    @Test
    @DisplayName("concluída antes do fim planejado e no próprio dia do fim está no prazo")
    void noPrazo() {
        assertThat(ordem(StatusOrdemProducao.CONCLUIDA, Instant.parse("2026-03-05T15:00:00Z")).foiConcluidaNoPrazo(SAO_PAULO)).isTrue();
        assertThat(ordem(StatusOrdemProducao.CONCLUIDA, Instant.parse("2026-03-09T20:00:00Z")).foiConcluidaNoPrazo(SAO_PAULO)).isTrue();
    }

    @Test
    @DisplayName("concluída um dia depois do fim planejado está com atraso")
    void comAtraso() {
        assertThat(ordem(StatusOrdemProducao.CONCLUIDA, Instant.parse("2026-03-10T15:00:00Z")).foiConcluidaNoPrazo(SAO_PAULO)).isFalse();
    }

    @Test
    @DisplayName("o dia vem do fuso: 02:30 UTC do dia 10 ainda é 23:30 do dia 9 em São Paulo, então está no prazo")
    void dependeDoFuso() {
        OrdemProducao ordem = ordem(StatusOrdemProducao.CONCLUIDA, Instant.parse("2026-03-10T02:30:00Z"));

        assertThat(ordem.foiConcluidaNoPrazo(SAO_PAULO)).isTrue();
        assertThat(ordem.foiConcluidaNoPrazo(ZoneId.of("UTC"))).isFalse();
    }

    @Test
    @DisplayName("ordem que não está concluída nunca é 'concluída no prazo'")
    void soConcluidas() {
        for (StatusOrdemProducao status : new StatusOrdemProducao[] {
                StatusOrdemProducao.PLANEJADA, StatusOrdemProducao.LIBERADA,
                StatusOrdemProducao.EM_PRODUCAO, StatusOrdemProducao.CANCELADA}) {
            assertThat(ordem(status, Instant.parse("2026-03-05T15:00:00Z")).foiConcluidaNoPrazo(SAO_PAULO)).isFalse();
        }
    }
}
