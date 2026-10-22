package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do domínio: JUnit puro, sem Spring, sem banco.
 * Rodam em milissegundos — é o nível onde a maioria das regras deve ser testada.
 */
class OrdemProducaoTest {

    private static final String USUARIO = "teste@pcp";

    private static final LocalDate INICIO = LocalDate.of(2026, 8, 10);
    private static final LocalDate FIM = LocalDate.of(2026, 8, 20);
    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID LISTA_ID = UUID.randomUUID();

    private OrdemProducao ordemValida() {
        return OrdemProducao.criar("OP-0001", MATERIAL_ID, LISTA_ID, null, "Usinagem CNC", 100, INICIO, FIM, USUARIO);
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
            assertThat(ordem.getMaterialId()).isEqualTo(MATERIAL_ID);
            assertThat(ordem.getListaTecnicaId()).isEqualTo(LISTA_ID);
            assertThat(ordem.getCentroDeTrabalho()).isEqualTo("Usinagem CNC");
            assertThat(ordem.getCriadaEm()).isNotNull();
        }

        @Test
        @DisplayName("rejeita quantidade zero ou negativa")
        void rejeitaQuantidadeInvalida() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("OP-0001", MATERIAL_ID, LISTA_ID, null, "Montagem", 0, INICIO, FIM, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade");
        }

        @Test
        @DisplayName("rejeita fim planejado anterior ao início")
        void rejeitaPeriodoInvalido() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("OP-0001", MATERIAL_ID, LISTA_ID, null, "Montagem", 10, FIM, INICIO, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("anterior");
        }

        @Test
        @DisplayName("rejeita código em branco")
        void rejeitaCodigoEmBranco() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("  ", MATERIAL_ID, LISTA_ID, null, "Montagem", 10, INICIO, FIM, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("código");
        }

        @Test
        @DisplayName("rejeita centro de trabalho em branco")
        void rejeitaCentroDeTrabalhoEmBranco() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("OP-0001", MATERIAL_ID, LISTA_ID, null, "  ", 10, INICIO, FIM, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("centro de trabalho");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("OP-0001", null, LISTA_ID, null, "Montagem", 10, INICIO, FIM, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("material");
        }

        @Test
        @DisplayName("rejeita lista técnica nula")
        void rejeitaListaTecnicaNula() {
            assertThatThrownBy(() ->
                    OrdemProducao.criar("OP-0001", MATERIAL_ID, null, null, "Montagem", 10, INICIO, FIM, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("lista técnica");
        }
    }

    @Nested
    @DisplayName("Máquina de estados")
    class MaquinaDeEstados {

        @Test
        @DisplayName("segue o fluxo normal até EM_PRODUCAO via alterarStatusPara")
        void fluxoNormalAteEmProducao() {
            OrdemProducao ordem = ordemValida();

            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);

            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.EM_PRODUCAO);
        }

        @Test
        @DisplayName("não permite CONCLUIDA via alterarStatusPara — deve usar concluir()")
        void naoPermiteConcluirViaAlterarStatus() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);

            assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.CONCLUIDA, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Transição de status inválida");
        }

        @Test
        @DisplayName("não permite pular etapas (PLANEJADA -> CONCLUIDA)")
        void naoPermitePularEtapas() {
            OrdemProducao ordem = ordemValida();

            assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.CONCLUIDA, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Transição de status inválida");
        }

        @Test
        @DisplayName("permite cancelar antes de concluir")
        void permiteCancelar() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);

            ordem.alterarStatusPara(StatusOrdemProducao.CANCELADA, USUARIO);

            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.CANCELADA);
        }

        @Test
        @DisplayName("não permite reabrir ordem cancelada")
        void naoPermiteReabrirCancelada() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.CANCELADA, USUARIO);

            assertThatThrownBy(() -> ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Conclusão")
    class Conclusao {

        @Test
        @DisplayName("conclui ordem EM_PRODUCAO com quantidade produzida")
        void concluiOrdemEmProducao() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);

            ordem.concluir(new BigDecimal("95"), USUARIO);

            assertThat(ordem.getStatus()).isEqualTo(StatusOrdemProducao.CONCLUIDA);
            assertThat(ordem.getQuantidadeProduzida()).isEqualByComparingTo(new BigDecimal("95"));
        }

        @Test
        @DisplayName("rejeita conclusão de ordem que não está EM_PRODUCAO")
        void rejeitaConclusaoForaDeEmProducao() {
            OrdemProducao ordem = ordemValida();

            assertThatThrownBy(() -> ordem.concluir(new BigDecimal("100"), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("EM_PRODUCAO");
        }

        @Test
        @DisplayName("rejeita quantidade produzida zero")
        void rejeitaQuantidadeProduzidaZero() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);

            assertThatThrownBy(() -> ordem.concluir(BigDecimal.ZERO, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade produzida");
        }

        @Test
        @DisplayName("rejeita quantidade produzida nula")
        void rejeitaQuantidadeProduzidaNula() {
            OrdemProducao ordem = ordemValida();
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);

            assertThatThrownBy(() -> ordem.concluir(null, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade produzida");
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
            ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
            ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);
            ordem.concluir(new BigDecimal("100"), USUARIO);

            assertThat(ordem.estaAtrasada(FIM.plusDays(30))).isFalse();
        }
    }
}
