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

    private static final String USUARIO = "teste@pcp";

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID ORDEM_ID = UUID.randomUUID();
    private static final LocalDate FABRICACAO = LocalDate.of(2026, 9, 1);
    private static final LocalDate VALIDADE = LocalDate.of(2027, 9, 1);

    private Lote loteValido() {
        return Lote.criar("MAT-ACO-1020-202609-001", MATERIAL_ID, ORDEM_ID,
                new BigDecimal("100.0000"), "kg", FABRICACAO, VALIDADE, USUARIO);
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
                            BigDecimal.ZERO, "kg", FABRICACAO, VALIDADE, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade");
        }

        @Test
        @DisplayName("rejeita validade anterior à fabricação")
        void rejeitaValidadeAnterior() {
            assertThatThrownBy(() ->
                    Lote.criar("MAT-001", MATERIAL_ID, ORDEM_ID,
                            new BigDecimal("10"), "kg", VALIDADE, FABRICACAO, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("validade");
        }

        @Test
        @DisplayName("rejeita número do lote em branco")
        void rejeitaNumeroEmBranco() {
            assertThatThrownBy(() ->
                    Lote.criar("  ", MATERIAL_ID, ORDEM_ID,
                            new BigDecimal("10"), "kg", FABRICACAO, VALIDADE, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("número do lote");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() ->
                    Lote.criar("MAT-001", null, ORDEM_ID,
                            new BigDecimal("10"), "kg", FABRICACAO, VALIDADE, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("material");
        }

        @Test
        @DisplayName("aceita ordem de produção nula (entrada manual futura)")
        void aceitaOrdemNula() {
            Lote lote = Lote.criar("MAT-001", MATERIAL_ID, null,
                    new BigDecimal("10"), "kg", FABRICACAO, VALIDADE, USUARIO);

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

            lote.bloquear(USUARIO);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.BLOQUEADO);
        }

        @Test
        @DisplayName("não bloqueia lote já consumido")
        void naoBloqueiaConsumido() {
            Lote lote = loteValido();
            lote.marcarComoConsumido(USUARIO);

            assertThatThrownBy(() -> lote.bloquear(USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("disponíveis");
        }

        @Test
        @DisplayName("marca lote disponível como vencido")
        void venceDisponivel() {
            Lote lote = loteValido();

            lote.marcarComoVencido(USUARIO);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.VENCIDO);
        }

        @Test
        @DisplayName("marca lote bloqueado como vencido")
        void venceBloqueado() {
            Lote lote = loteValido();
            lote.bloquear(USUARIO);

            lote.marcarComoVencido(USUARIO);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.VENCIDO);
        }

        @Test
        @DisplayName("consome lote disponível")
        void consomeDisponivel() {
            Lote lote = loteValido();

            lote.marcarComoConsumido(USUARIO);

            assertThat(lote.getStatus()).isEqualTo(StatusLote.CONSUMIDO);
        }

        @Test
        @DisplayName("não consome lote bloqueado")
        void naoConsomeBloqueado() {
            Lote lote = loteValido();
            lote.bloquear(USUARIO);

            assertThatThrownBy(() -> lote.marcarComoConsumido(USUARIO))
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
            Lote lote = Lote.receberCompra("MAT-MP-001-202609-001", MATERIAL_ID, origem("NF-1"),
                    new BigDecimal("10"), "kg",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2027, 9, 1), USUARIO);

            assertThat(lote.ehDeCompra()).isTrue();
            assertThat(lote.getOrdemProducaoId()).isNull();
            assertThat(lote.getStatus()).isEqualTo(StatusLote.DISPONIVEL);
            assertThat(lote.getNotaFiscal()).isEqualTo("NF-1");
            assertThat(lote.getOrigemCompra().dataEmissaoNf()).isEqualTo(LocalDate.of(2026, 9, 2));
            assertThat(lote.getOrigemCompra().dataRecebimento()).isEqualTo(LocalDate.of(2026, 9, 3));
            assertThat(lote.getAssinatura().criadoPor()).isEqualTo(USUARIO);
        }

        @Test
        @DisplayName("recebimento anterior à emissão da nota fiscal é rejeitado")
        void recebimentoAntesDaEmissao() {
            assertThatThrownBy(() -> new OrigemCompra("Fornecedor", "NF-1",
                    LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 2)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("anterior à emissão");
        }

        @Test
        @DisplayName("datas da nota fiscal são obrigatórias")
        void datasObrigatorias() {
            assertThatThrownBy(() -> new OrigemCompra("Fornecedor", "NF-1", null, LocalDate.of(2026, 9, 2)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("emissão");
            assertThatThrownBy(() -> new OrigemCompra("Fornecedor", "NF-1", LocalDate.of(2026, 9, 2), null))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("recebimento");
        }

        @Test
        @DisplayName("fabricação posterior ao recebimento é rejeitada")
        void fabricacaoDepoisDoRecebimento() {
            assertThatThrownBy(() -> Lote.receberCompra("MAT-1", MATERIAL_ID, origem("NF-1"),
                    new BigDecimal("10"), "kg",
                    LocalDate.of(2026, 9, 4), LocalDate.of(2027, 9, 1), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("fabricação");
        }

        @Test
        @DisplayName("lote de produção não é de compra")
        void loteDeProducao() {
            assertThat(loteValido().ehDeCompra()).isFalse();
        }

        @Test
        @DisplayName("nota fiscal acima de 44 caracteres é rejeitada")
        void notaFiscalLonga() {
            assertThatThrownBy(() -> origem("9".repeat(45)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("44");
        }

        private OrigemCompra origem(String notaFiscal) {
            return new OrigemCompra("Fornecedor", notaFiscal,
                    LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 3));
        }
    }
}
