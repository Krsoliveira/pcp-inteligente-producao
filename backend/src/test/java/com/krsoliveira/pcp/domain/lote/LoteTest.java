package com.krsoliveira.pcp.domain.lote;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoteTest {

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID ORDEM_ID = UUID.randomUUID();
    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 1);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 1);

    private Lote loteValido() {
        return Lote.criar("MAT-ACO-1020-202609-001", MATERIAL_ID, ORDEM_ID,
                new BigDecimal("100.0000"), "kg", FABRICACAO, VALIDADE);
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("cria lote válido com status DISPONIVEL")
        void criaLoteValido() {
            Lote lote = loteValido();

            assertThat(lote.getId()).isNotNull();
            assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
            assertThat(lote.getNumeroLote()).isEqualTo("MAT-ACO-1020-202609-001");
            assertThat(lote.getMaterialId()).isEqualTo(MATERIAL_ID);
            assertThat(lote.getOrdemProducaoId()).isEqualTo(ORDEM_ID);
            assertThat(lote.getQuantidade()).isEqualByComparingTo(new BigDecimal("100.0000"));
        }

        @Test
        @DisplayName("rejeita quantidade zero ou negativa")
        void rejeitaQuantidadeInvalida() {
            assertThatThrownBy(() ->
                    Lote.criar("MAT-001", MATERIAL_ID, ORDEM_ID,
                            BigDecimal.ZERO, "kg", FABRICACAO, VALIDADE))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade");
        }

        @Test
        @DisplayName("rejeita validade anterior à fabricação")
        void rejeitaValidadeAnterior() {
            assertThatThrownBy(() ->
                    Lote.criar("MAT-001", MATERIAL_ID, ORDEM_ID,
                            new BigDecimal("10"), "kg", VALIDADE, FABRICACAO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("validade");
        }

        @Test
        @DisplayName("rejeita número do lote em branco")
        void rejeitaNumeroEmBranco() {
            assertThatThrownBy(() ->
                    Lote.criar("  ", MATERIAL_ID, ORDEM_ID,
                            new BigDecimal("10"), "kg", FABRICACAO, VALIDADE))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("número do lote");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() ->
                    Lote.criar("MAT-001", null, ORDEM_ID,
                            new BigDecimal("10"), "kg", FABRICACAO, VALIDADE))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("material");
        }

        @Test
        @DisplayName("aceita ordem de produção nula (entrada manual futura)")
        void aceitaOrdemNula() {
            Lote lote = Lote.criar("MAT-001", MATERIAL_ID, null,
                    new BigDecimal("10"), "kg", FABRICACAO, VALIDADE);

            assertThat(lote.getOrdemProducaoId()).isNull();
        }
    }

    @Nested
    @DisplayName("Transições de status")
    class TransicoesDeStatus {

        @Test
        @DisplayName("bloqueia lote disponível")
        void bloqueiaDisponivel() {
            Lote lote = loteValido();

            lote.bloquear();

            assertThat(lote.getStatus()).isEqualTo(StatusLote.BLOQUEADO);
        }

        @Test
        @DisplayName("não bloqueia lote já consumido")
        void naoBloqueiaConsumido() {
            Lote lote = loteValido();
            lote.marcarComoConsumido();

            assertThatThrownBy(lote::bloquear)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("disponíveis");
        }

        @Test
        @DisplayName("marca lote disponível como vencido")
        void venceDisponivel() {
            Lote lote = loteValido();

            lote.marcarComoVencido();

            assertThat(lote.getStatus()).isEqualTo(StatusLote.VENCIDO);
        }

        @Test
        @DisplayName("marca lote bloqueado como vencido")
        void venceBloqueado() {
            Lote lote = loteValido();
            lote.bloquear();

            lote.marcarComoVencido();

            assertThat(lote.getStatus()).isEqualTo(StatusLote.VENCIDO);
        }

        @Test
        @DisplayName("consome lote disponível")
        void consomeDisponivel() {
            Lote lote = loteValido();

            lote.marcarComoConsumido();

            assertThat(lote.getStatus()).isEqualTo(StatusLote.CONSUMIDO);
        }

        @Test
        @DisplayName("não consome lote bloqueado")
        void naoConsomeBloqueado() {
            Lote lote = loteValido();
            lote.bloquear();

            assertThatThrownBy(lote::marcarComoConsumido)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("disponíveis");
        }
    }

    @Nested
    @DisplayName("Entrada por compra")
    class EntradaPorCompra {

        @Test
        @DisplayName("lote de compra guarda fornecedor e nota fiscal e não tem ordem")
        void loteDeCompra() {
            Lote lote = Lote.receberCompra("MAT-MP-001-202609-001", MATERIAL_ID, "Fornecedor",
                    "NF-1", new BigDecimal("10"), "kg",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1));

            assertThat(lote.ehDeCompra()).isTrue();
            assertThat(lote.getOrdemProducaoId()).isNull();
            assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
        }

        @Test
        @DisplayName("lote de produção não é de compra")
        void loteDeProducao() {
            assertThat(loteValido().ehDeCompra()).isFalse();
        }

        @Test
        @DisplayName("nota fiscal acima de 44 caracteres é rejeitada")
        void notaFiscalLonga() {
            assertThatThrownBy(() -> Lote.receberCompra("MAT-1", MATERIAL_ID, "Fornecedor",
                    "9".repeat(45), new BigDecimal("10"), "kg",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("44");
        }
    }
}
