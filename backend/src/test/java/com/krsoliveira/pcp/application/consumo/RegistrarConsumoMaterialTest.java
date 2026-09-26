package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarConsumoMaterialTest {

    private ConsumoMaterialRepositoryEmMemoria repositorio;
    private RegistrarConsumoMaterial casoDeUso;

    @BeforeEach
    void setUp() {
        repositorio = new ConsumoMaterialRepositoryEmMemoria();
        casoDeUso = new RegistrarConsumoMaterial(repositorio);
    }

    @Test
    @DisplayName("registra consumo sem desvio com sucesso")
    void registraConsumoSemDesvio() {
        ConsumoMaterial consumo = criarConsumoProjetado(new BigDecimal("50.00"));
        repositorio.salvar(consumo);

        ConsumoMaterial resultado = casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                consumo.getId(), new BigDecimal("50.00"), null, null));

        assertThat(resultado.estaRegistrado()).isTrue();
        assertThat(resultado.getDesvio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("registra consumo com desvio e justificativa")
    void registraConsumoComDesvio() {
        ConsumoMaterial consumo = criarConsumoProjetado(new BigDecimal("50.00"));
        repositorio.salvar(consumo);

        ConsumoMaterial resultado = casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                consumo.getId(), new BigDecimal("55.00"),
                "Perda no setup", "João Silva"));

        assertThat(resultado.estaRegistrado()).isTrue();
        assertThat(resultado.getDesvio()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(resultado.getJustificativa()).isEqualTo("Perda no setup");
        assertThat(resultado.estaJustificado()).isTrue();
    }

    @Test
    @DisplayName("rejeita registro de consumo não encontrado")
    void rejeitaConsumoNaoEncontrado() {
        assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                UUID.randomUUID(), new BigDecimal("10"), null, null)))
                .isInstanceOf(ConsumoMaterialNaoEncontradoException.class);
    }

    @Test
    @DisplayName("delega validação de desvio sem justificativa ao domínio")
    void delegaValidacaoDesvioAoDominio() {
        ConsumoMaterial consumo = criarConsumoProjetado(new BigDecimal("50.00"));
        repositorio.salvar(consumo);

        assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                consumo.getId(), new BigDecimal("60.00"), null, null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Justificativa");
    }

    private ConsumoMaterial criarConsumoProjetado(BigDecimal qtdPlanejada) {
        return ConsumoMaterial.projetar(
                UUID.randomUUID(), UUID.randomUUID(), qtdPlanejada, "kg");
    }
}
