package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.application.lote.LoteRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarResumoProducaoTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

    private OrdemProducaoRepositoryEmMemoria ordens;
    private LoteRepositoryEmMemoria lotes;
    private ConsultarResumoProducao casoDeUso;

    @BeforeEach
    void setUp() {
        ordens = new OrdemProducaoRepositoryEmMemoria();
        lotes = new LoteRepositoryEmMemoria();
        casoDeUso = new ConsultarResumoProducao(ordens, lotes);
    }

    @Test
    @DisplayName("sem dados, todos os status aparecem com zero")
    void semDados() {
        ResumoProducao resumo = casoDeUso.executar(HOJE);

        assertThat(resumo.totalOrdens()).isZero();
        assertThat(resumo.ordensPorStatus()).containsOnlyKeys(StatusOrdemProducao.values());
        assertThat(resumo.ordensPorStatus().values()).containsOnly(0L);
        assertThat(resumo.ordensAtrasadas()).isZero();
        assertThat(resumo.lotesDisponiveis()).isZero();
    }

    @Test
    @DisplayName("conta ordens por status e o total")
    void contaPorStatus() {
        ordens.salvar(ordem("OP-1", LocalDate.of(2027, 1, 1)));
        ordens.salvar(ordem("OP-2", LocalDate.of(2027, 1, 1)));
        var liberada = ordem("OP-3", LocalDate.of(2027, 1, 1));
        liberada.alterarStatusPara(StatusOrdemProducao.LIBERADA, "teste");
        ordens.salvar(liberada);

        ResumoProducao resumo = casoDeUso.executar(HOJE);

        assertThat(resumo.totalOrdens()).isEqualTo(3);
        assertThat(resumo.ordensPorStatus()).containsEntry(StatusOrdemProducao.PLANEJADA, 2L)
                .containsEntry(StatusOrdemProducao.LIBERADA, 1L)
                .containsEntry(StatusOrdemProducao.CONCLUIDA, 0L);
    }

    @Test
    @DisplayName("atrasada é ordem aberta com fim planejado já vencido; ordem cancelada não conta")
    void contaAtrasadas() {
        ordens.salvar(ordem("OP-1", HOJE.minusDays(1)));
        ordens.salvar(ordem("OP-2", HOJE));
        var cancelada = ordem("OP-3", HOJE.minusDays(30));
        cancelada.alterarStatusPara(StatusOrdemProducao.CANCELADA, "teste");
        ordens.salvar(cancelada);

        assertThat(casoDeUso.executar(HOJE).ordensAtrasadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("conta os lotes disponíveis")
    void contaLotesDisponiveis() {
        lotes.salvar(lote("L1"));
        lotes.salvar(lote("L2"));

        assertThat(casoDeUso.executar(HOJE).lotesDisponiveis()).isEqualTo(2);
    }

    private static OrdemProducao ordem(String codigo, LocalDate fimPlanejado) {
        return OrdemProducao.criar(codigo, UUID.randomUUID(), UUID.randomUUID(), null, "Usinagem", 10,
                LocalDate.of(2020, 1, 1), fimPlanejado, "teste");
    }

    private static Lote lote(String numero) {
        return Lote.criar(numero, UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("10"), "un",
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), "teste");
    }
}
