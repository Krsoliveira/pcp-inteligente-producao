package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do domínio: JUnit puro, sem Spring, sem banco.
 * Rodam em milissegundos — é o nível onde a maioria das regras deve ser testada.
 */
class OrdemProducaoTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 10);
    private static final LocalDate FIM = LocalDate.of(2026, 8, 20);

    private OrdemProducao ordemValida() {
        return OrdemProducao.criar("OP-0001", "Viga metálica 6m", "Usinagem CNC", 100, INICIO, FIM);
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("cria ordem válida com status PLANEJADA")
        void criaOrdemValida() {
            OrdemProducao ordem = ordemValida();

            assertThat(ordem.getId()).isNotNull();
            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.PLANEJADA);
            assertThat(ordem.getCodigo()).isEqualTo("OP-0001");
            assertThat(ordem.getCentroDeTrabalho()).isEqualTo("Usinagem CNC");
            assertThat(ordem.getCriadaEm()).isNotNull();
        }

        @Test
        @DisplayName("rejeita quantidade zero ou negativa")
        void rejeitaQuantidadeInvalida() {
            assertThatThrownBy(() -> OrdemProducao.criar("OP-0001", "Viga", "Montagem", 0, INICIO, FIM))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade");
        }

        @Test
        @DisplayName("rejeita fim planejado anterior ao início")
        void rejeitaPeriodoInvalido() {
            assertThatThrownBy(() -> OrdemProducao.criar("OP-0001", "Viga", "Montagem", 10, FIM, INICIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("anterior");
        }

        @Test
        @DisplayName("rejeita código em branco")
        void rejeitaCodigoEmBranco() {
            assertThatThrownBy(() -> OrdemProducao.criar("  ", "Viga", "Montagem", 10, INICIO, FIM))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("código");
        }

        @Test
        @DisplayName("rejeita centro de trabalho em branco")
        void rejeitaCentroDeTrabalhoEmBranco() {
            assertThatThrownBy(() -> OrdemProducao.criar("OP-0001", "Viga", "  ", 10, INICIO, FIM))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("centro de trabalho");
        }
    }

    @Nested
    @DisplayName("Máquina de estados")
    class MaquinaDeEstados {

        @Test
        @DisplayName("segue o fluxo normal: PLANEJADA -> LIBERADA -> EM_PRODUCAO -> CONCLUIDA")
        void fluxoNormal() {
            OrdemProducao ordem = ordemValida();

            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO);
            ordem.alterarStatusPara(StatusOrdemProducao.CONCLUIDA);

            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.CONCLUIDA);
        }

        @Test
        @DisplayName("não permite pular etapas (PLANEJADA -> CONCLUIDA)")
        void naoPermitePularEtapas() {
            OrdemProducao ordem = ordemValida();

            assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.CONCLUIDA))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Transição de status inválida");
        }

        @Test
        @DisplayName("permite cancelar antes de concluir")
        void permiteCancelar() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA);

            ordem.alterarStatusPara(StatusOrdemProducao.CANCELADA);

            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.CANCELADA);
        }

        @Test
        @DisplayName("não permite reabrir ordem cancelada")
        void naoPermiteReabrirCancelada() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.CANCELADA);

            assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Atraso")
    class Atraso {

        @Test
        @DisplayName("ordem aberta após o fim planejado está atrasada")
        void ordemAbertaAposFimEstaAtrasada() {
            OrdemProducao ordem = ordemValida();

            assertThat(ordem.estaAtrasada(FIM.plusDays(1))).isTrue();
        }

        @Test
        @DisplayName("ordem dentro do prazo não está atrasada")
        void ordemNoPrazoNaoEstaAtrasada() {
            OrdemProducao ordem = ordemValida();

            assertThat(ordem.estaAtrasada(FIM)).isFalse();
        }

        @Test
        @DisplayName("ordem concluída nunca conta como atrasada")
        void ordemConcluidaNaoContaComoAtrasada() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO);
            ordem.alterarStatusPara(StatusOrdemProducao.CONCLUIDA);

            assertThat(ordem.estaAtrasada(FIM.plusDays(30))).isFalse();
        }
    }
}
