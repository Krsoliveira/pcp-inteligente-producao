package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarEntregasPorMesTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private OrdemProducaoRepositoryEmMemoria ordens;
    private ConsultarEntregasPorMes casoDeUso;

    @BeforeEach
    void setUp() {
        ordens = new OrdemProducaoRepositoryEmMemoria();
        casoDeUso = new ConsultarEntregasPorMes(ordens);
    }

    private OrdemProducao salvar(String codigo, StatusOrdemProducao status, LocalDate fim, Instant alteradaEm) {
        Assinatura assinatura = new Assinatura("ana", Instant.parse("2026-01-01T12:00:00Z"), "ana", alteradaEm);
        return ordens.salvar(OrdemProducao.reconstituir(UUID.randomUUID(), codigo, UUID.randomUUID(), UUID.randomUUID(),
                null, "Usinagem", 10, BigDecimal.TEN, fim.minusDays(5), fim, status, assinatura));
    }

    private EntregasDoMes mes(List<EntregasDoMes> serie, String anoMes) {
        return serie.stream().filter(m -> m.mes().equals(YearMonth.parse(anoMes))).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("devolve um item por mês, do mais antigo ao atual, com zeros nos meses sem ordens")
    void serieContinua() {
        List<EntregasDoMes> serie = casoDeUso.executar(HOJE, FUSO, 12);

        assertThat(serie).hasSize(12);
        assertThat(serie.get(0).mes()).isEqualTo(YearMonth.of(2025, 11));
        assertThat(serie.get(11).mes()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(serie).allSatisfy(m -> assertThat(m.noPrazo() + m.comAtraso() + m.abertasAtrasadas() + m.abertasNoPrazo()).isZero());
    }

    @Test
    @DisplayName("classifica as ordens do mês do fim planejado em no prazo, com atraso, abertas atrasadas e abertas no prazo")
    void classifica() {
        // fim em setembro/2026
        salvar("OP-1", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10), Instant.parse("2026-09-09T15:00:00Z")); // no prazo
        salvar("OP-2", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10), Instant.parse("2026-09-10T20:00:00Z")); // no dia do fim
        salvar("OP-3", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10), Instant.parse("2026-09-12T15:00:00Z")); // com atraso
        salvar("OP-4", StatusOrdemProducao.EM_PRODUCAO, LocalDate.of(2026, 9, 20), Instant.parse("2026-09-01T15:00:00Z")); // aberta, passou do fim
        // fim em outubro/2026 (hoje é 21/10)
        salvar("OP-5", StatusOrdemProducao.LIBERADA, LocalDate.of(2026, 10, 21), Instant.parse("2026-10-01T15:00:00Z")); // aberta, fim é hoje: no prazo
        salvar("OP-6", StatusOrdemProducao.PLANEJADA, LocalDate.of(2026, 10, 30), Instant.parse("2026-10-01T15:00:00Z")); // aberta no prazo

        List<EntregasDoMes> serie = casoDeUso.executar(HOJE, FUSO, 12);

        EntregasDoMes setembro = mes(serie, "2026-09");
        assertThat(setembro.noPrazo()).isEqualTo(2);
        assertThat(setembro.comAtraso()).isEqualTo(1);
        assertThat(setembro.abertasAtrasadas()).isEqualTo(1);
        assertThat(setembro.abertasNoPrazo()).isZero();
        assertThat(setembro.concluidas()).isEqualTo(3);
        EntregasDoMes outubro = mes(serie, "2026-10");
        assertThat(outubro.abertasNoPrazo()).isEqualTo(2);
        assertThat(outubro.abertasAtrasadas()).isZero();
    }

    @Test
    @DisplayName("canceladas e ordens fora da janela não entram")
    void ignoraCanceladasEForaDaJanela() {
        salvar("OP-1", StatusOrdemProducao.CANCELADA, LocalDate.of(2026, 9, 10), Instant.parse("2026-09-09T15:00:00Z"));
        salvar("OP-2", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2025, 1, 10), Instant.parse("2025-01-09T15:00:00Z")); // antes da janela
        salvar("OP-3", StatusOrdemProducao.PLANEJADA, LocalDate.of(2026, 11, 5), Instant.parse("2026-10-01T15:00:00Z")); // depois do mês atual

        List<EntregasDoMes> serie = casoDeUso.executar(HOJE, FUSO, 12);

        assertThat(serie).allSatisfy(m -> assertThat(m.noPrazo() + m.comAtraso() + m.abertasAtrasadas() + m.abertasNoPrazo()).isZero());
    }

    @Test
    @DisplayName("o fuso decide o dia da conclusão: 02:30 UTC do dia 11 é dia 10 em São Paulo, então está no prazo")
    void usaOFuso() {
        salvar("OP-1", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10), Instant.parse("2026-09-11T02:30:00Z"));

        assertThat(mes(casoDeUso.executar(HOJE, FUSO, 12), "2026-09").noPrazo()).isEqualTo(1);
        assertThat(mes(casoDeUso.executar(HOJE, ZoneId.of("UTC"), 12), "2026-09").comAtraso()).isEqualTo(1);
    }

    @Test
    @DisplayName("a quantidade de meses é limitada a 1..24 e o padrão é 12")
    void limitesDeMeses() {
        assertThat(casoDeUso.executar(HOJE, FUSO, null)).hasSize(12);
        assertThat(casoDeUso.executar(HOJE, FUSO, 0)).hasSize(1);
        assertThat(casoDeUso.executar(HOJE, FUSO, -5)).hasSize(1);
        assertThat(casoDeUso.executar(HOJE, FUSO, 3)).extracting(EntregasDoMes::mes)
                .containsExactly(YearMonth.of(2026, 8), YearMonth.of(2026, 9), YearMonth.of(2026, 10));
        assertThat(casoDeUso.executar(HOJE, FUSO, 1000)).hasSize(24);
    }
}
