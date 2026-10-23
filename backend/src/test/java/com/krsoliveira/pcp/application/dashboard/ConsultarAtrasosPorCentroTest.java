package com.krsoliveira.pcp.application.dashboard;

import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarAtrasosPorCentroTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 21);

    private OrdemProducaoRepositoryEmMemoria ordens;
    private ConsultarAtrasosPorCentro casoDeUso;

    @BeforeEach
    void setUp() {
        ordens = new OrdemProducaoRepositoryEmMemoria();
        casoDeUso = new ConsultarAtrasosPorCentro(ordens);
    }

    private OrdemProducao salvar(String codigo, String centro, LocalDate fim, StatusOrdemProducao destino) {
        OrdemProducao ordem = OrdemProducao.criar(codigo, UUID.randomUUID(), UUID.randomUUID(), null, centro, 10,
                fim.minusDays(5), fim, "teste");
        if (destino == StatusOrdemProducao.CANCELADA) ordem.alterarStatusPara(StatusOrdemProducao.CANCELADA, "teste");
        return ordens.salvar(ordem);
    }

    @Test
    @DisplayName("sem ordens atrasadas devolve lista vazia")
    void semAtrasos() {
        salvar("OP-1", "Usinagem", HOJE.plusDays(3), StatusOrdemProducao.PLANEJADA);

        assertThat(casoDeUso.executar(HOJE)).isEmpty();
    }

    @Test
    @DisplayName("agrupa por centro, ordena do que tem mais atrasadas para o que tem menos e calcula os dias da mais antiga")
    void agrupaEOrdena() {
        salvar("OP-1", "Soldagem", HOJE.minusDays(2), StatusOrdemProducao.PLANEJADA);
        salvar("OP-2", "Usinagem", HOJE.minusDays(10), StatusOrdemProducao.PLANEJADA);
        salvar("OP-3", "Usinagem", HOJE.minusDays(30), StatusOrdemProducao.PLANEJADA);
        salvar("OP-4", "Usinagem", HOJE.minusDays(1), StatusOrdemProducao.PLANEJADA);

        List<AtrasoDoCentro> atrasos = casoDeUso.executar(HOJE);

        assertThat(atrasos).containsExactly(
                new AtrasoDoCentro("Usinagem", 3, 30),
                new AtrasoDoCentro("Soldagem", 1, 2));
    }

    @Test
    @DisplayName("empate na quantidade sai pelo nome do centro; canceladas e o próprio dia do fim não contam")
    void empateECanceladas() {
        salvar("OP-1", "Pintura", HOJE.minusDays(4), StatusOrdemProducao.PLANEJADA);
        salvar("OP-2", "Montagem", HOJE.minusDays(4), StatusOrdemProducao.PLANEJADA);
        salvar("OP-3", "Montagem", HOJE.minusDays(9), StatusOrdemProducao.CANCELADA);
        salvar("OP-4", "Montagem", HOJE, StatusOrdemProducao.PLANEJADA); // fim é hoje: ainda não está atrasada

        assertThat(casoDeUso.executar(HOJE)).extracting(AtrasoDoCentro::centroDeTrabalho)
                .containsExactly("Montagem", "Pintura");
        assertThat(casoDeUso.executar(HOJE)).extracting(AtrasoDoCentro::ordensAtrasadas).containsExactly(1L, 1L);
    }
}
