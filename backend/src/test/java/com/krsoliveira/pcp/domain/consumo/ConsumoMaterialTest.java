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

    private static final String USUARIO = "teste@pcp";

    private static final UUID ORDEM_ID = UUID.randomUUID();
    private static final UUID MATERIAL_ID = UUID.randomUUID();

    private ConsumoMaterial consumoProjetado() {
        return ConsumoMaterial.projetar(ORDEM_ID, MATERIAL_ID,
                new BigDecimal("50.0000"), "kg", USUARIO);
    }

    @Nested
    @DisplayName("Item não planejado (ADR-0025)")
    class NaoPlanejado {

        private ConsumoMaterial naoPlanejado() {
            return ConsumoMaterial.naoPlanejado(ORDEM_ID, MATERIAL_ID, " un ", USUARIO);
        }

        @Test
        @DisplayName("nasce com planejado zero, sem consumo registrado, e é reconhecido como não planejado")
        void nasceComPlanejadoZero() {
            ConsumoMaterial consumo = naoPlanejado();

            assertThat(consumo.isNaoPlanejado()).isTrue();
            assertThat(consumo.getQuantidadePlanejada()).isEqualByComparingTo("0");
            assertThat(consumo.getQuantidadeConsumida()).isNull();
            assertThat(consumo.getUnidadeDeMedida()).isEqualTo("un");
            assertThat(consumo.estaRegistrado()).isFalse();
        }

        @Test
        @DisplayName("as linhas projetadas da lista nunca são não planejadas")
        void projetadoNaoEhNaoPlanejado() {
            assertThat(consumoProjetado().isNaoPlanejado()).isFalse();
        }

        @Test
        @DisplayName("tudo o que se consome é desvio, então registrar sem justificativa é recusado")
        void desvioExigeJustificativa() {
            ConsumoMaterial consumo = naoPlanejado();

            assertThatThrownBy(() -> consumo.registrarConsumo(new BigDecimal("3"), " ", USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Justificativa obrigatória");
            assertThat(consumo.estaRegistrado()).isFalse();
        }

        @Test
        @DisplayName("com justificativa, registra e o desvio é igual ao consumido")
        void registraComJustificativa() {
            ConsumoMaterial consumo = naoPlanejado();

            consumo.registrarConsumo(new BigDecimal("3"), "Faltou na lista", USUARIO);

            assertThat(consumo.getDesvio()).isEqualByComparingTo("3");
            assertThat(consumo.estaJustificado()).isTrue();
            assertThat(consumo.getJustificadoPor()).isEqualTo(USUARIO);
        }

        @Test
        @DisplayName("exige ordem, material e unidade")
        void exigeDados() {
            assertThatThrownBy(() -> ConsumoMaterial.naoPlanejado(null, MATERIAL_ID, "un", USUARIO)).isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> ConsumoMaterial.naoPlanejado(ORDEM_ID, null, "un", USUARIO)).isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> ConsumoMaterial.naoPlanejado(ORDEM_ID, MATERIAL_ID, " ", USUARIO)).isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
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
                    ConsumoMaterial.projetar(ORDEM_ID, MATERIAL_ID, BigDecimal.ZERO, "kg", USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade planejada");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() ->
                    ConsumoMaterial.projetar(ORDEM_ID, null, new BigDecimal("10"), "kg", USUARIO))
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

            consumo.registrarConsumo(new BigDecimal("50.0000"), null, USUARIO);

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
                    "Perda no setup da máquina", "joao@pcp");

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(consumo.getDesvio()).isEqualByComparingTo(new BigDecimal("5.0000"));
            assertThat(consumo.estaJustificado()).isTrue();
            assertThat(consumo.getJustificativa()).isEqualTo("Perda no setup da máquina");
            assertThat(consumo.getJustificadoPor()).isEqualTo("joao@pcp");
            assertThat(consumo.getJustificadoEm()).isNotNull();
            assertThat(consumo.getAssinatura().alteradoPor()).isEqualTo("joao@pcp");
            assertThat(consumo.getAssinatura().criadoPor()).isEqualTo(USUARIO);
        }

        @Test
        @DisplayName("rejeita desvio sem justificativa")
        void rejeitaDesvioSemJustificativa() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("60.0000"), null, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Justificativa obrigatória");
        }

        @Test
        @DisplayName("rejeita registro sem usuário responsável")
        void rejeitaRegistroSemUsuario() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("60.0000"),
                            "Perda de material", " "))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("usuário responsável");
        }

        @Test
        @DisplayName("o registro é único: o consumo já baixou saldo dos lotes")
        void registroUnico() {
            ConsumoMaterial consumo = consumoProjetado();
            consumo.registrarConsumo(new BigDecimal("55.0000"), "Perda no setup", USUARIO);

            assertThatThrownBy(() -> consumo.registrarConsumo(new BigDecimal("50.0000"), null, "outro@pcp"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já foi registrado");
            assertThat(consumo.getQuantidadeConsumida()).isEqualByComparingTo("55");
            assertThat(consumo.getAssinatura().alteradoPor()).isEqualTo(USUARIO);
        }

        @Test
        @DisplayName("rejeita quantidade consumida negativa")
        void rejeitaQuantidadeNegativa() {
            ConsumoMaterial consumo = consumoProjetado();

            assertThatThrownBy(() ->
                    consumo.registrarConsumo(new BigDecimal("-1"), null, USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade consumida");
        }

        @Test
        @DisplayName("aceita quantidade consumida zero (material não utilizado)")
        void aceitaQuantidadeZero() {
            ConsumoMaterial consumo = consumoProjetado();

            consumo.registrarConsumo(BigDecimal.ZERO,
                    "Material substituído por alternativo", USUARIO);

            assertThat(consumo.estaRegistrado()).isTrue();
            assertThat(consumo.getDesvio()).isEqualByComparingTo(new BigDecimal("-50.0000"));
            assertThat(consumo.estaJustificado()).isTrue();
        }
    }
}
