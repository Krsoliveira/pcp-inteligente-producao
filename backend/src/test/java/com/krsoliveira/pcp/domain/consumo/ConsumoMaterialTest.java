package com.krsoliveira.pcp.domain.consumo;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumoMaterialTest {

    private static final UUID ORDEM_ID = UUID.randomUUID();
    private static final UUID MATERIAL_ID = UUID.randomUUID();

    private ConsumoMaterial consumoProjetado() {
        return ConsumoMaterial.projetar(ORDEM_ID, MATERIAL_ID,
                new BigDecimal("50.0000"), "kg");
    }

    @Nested
    @DisplayName("Projeção")
    class Projecao {

        @Test
        @DisplayName("projeta consumo com quantidade consumida nula")
        void projetaConsumo() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThat(consumo.getId()).isNotNull();
            assertThat(consumo.getOrdemProducaoId()).isEqualTo(ORDEM_ID);
            assertThat(consumo.getMaterialId()).isEqualTo(MATERIAL_ID);
            assertThat(consumo.getQuantidadePlanejada()).isEqualByComparingTo(new BigDecimal("50.0000"));
            assertThat(consumo.getQuantidadeConsumida()).isNull();
            assertThat(consumo.getDesvio()).isNull();
            assertThat(consumo.estaRegistrado()).isFalse();
            assertThat(consumo.estaJustificado()).isTrue(); // não registrado = nada a justificar
        }

        @Test
        @DisplayName("rejeita quantidade planejada zero ou negativa")
        void rejeitaQuantidadeInvalida() {
            assertThatThrownBy(() ->
                    ConsumoMaterial.projetar(ORDEM_ID, MATERIAL_ID, BigDecimal.ZERO, "kg"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade planejada");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() ->
                    ConsumoMaterial.projetar(ORDEM_ID, null, new BigDecimal("10"), "kg"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("material");
        }
    }

    @Nested
    @DisplayName("Registro de consumo")
    class RegistroDeConsumo {

        @Test
        @DisplayName("registra consumo sem desvio — sem justificativa necessária")
        void registraSemDesvio() {
            ConsumoMaterial consumo = consumoProjetado();

            consumo.registrarConsumo(new BigDecimal("50.0000"), null, null);

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(consumo.getDesvio()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(consumo.estaJustificado()).isTrue();
            assertThat(consumo.getJustificativa()).isNull();
        }

        @Test
        @DisplayName("registra consumo com desvio e justificativa")
        void registraComDesvioJustificado() {
            ConsumoMaterial consumo = consumoProjetado();

            consumo.registrarConsumo(new BigDecimal("55.0000"),
                    "Perda no setup da máquina", "João Silva");

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(consumo.getDesvio()).isEqualByComparingTo(new BigDecimal("5.0000"));
            assertThat(consumo.estaJustificado()).isTrue();
            assertThat(consumo.getJustificativa()).isEqualTo("Perda no setup da máquina");
            assertThat(consumo.getJustificadoPor()).isEqualTo("João Silva");
            assertThat(consumo.getJustificadoEm()).isNotNull();
        }

        @Test
        @DisplayName("rejeita desvio sem justificativa")
        void rejeitaDesvioSemJustificativa() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("60.0000"), null, null))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Justificativa obrigatória");
        }

        @Test
        @DisplayName("rejeita desvio com justificativa mas sem responsável")
        void rejeitaDesvioSemResponsavel() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("60.0000"),
                            "Perda de material", null))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Responsável");
        }

        @Test
        @DisplayName("rejeita quantidade consumida negativa")
        void rejeitaQuantidadeNegativa() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("-1"), null, null))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade consumida");
        }

        @Test
        @DisplayName("aceita quantidade consumida zero (material não utilizado)")
        void aceitaQuantidadeZero() {
            ConsumoMaterial consumo = consumoProjetado();

            consumo.registrarConsumo(BigDecimal.ZERO,
                    "Material substituído por alternativo", "Maria Santos");

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(consumo.getDesvio()).isEqualByComparingTo(new BigDecimal("-50.0000"));
            assertThat(consumo.estaJustificado()).isTrue();
        }
    }
}
