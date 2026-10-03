package com.krsoliveira.pcp.application.consumo;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarConsumoMaterialTest {

    private ConsumoMaterialRepositoryEmMemoria repositorio;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private RegistrarConsumoMaterial casoDeUso;

    @BeforeEach
    void setUp() {
        repositorio = new ConsumoMaterialRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        casoDeUso = new RegistrarConsumoMaterial(repositorio, new OrdemProducaoRepositoryEmMemoria(),
                new MaterialRepositoryEmMemoria(), trilha.execucao());
    }

    @Test
    @DisplayName("registra consumo sem desvio com sucesso")
    void registraConsumoSemDesvio() {
        ConsumoMaterial consumo = criarConsumoProjetado(new BigDecimal("50.00"));
        repositorio.salvar(consumo);

        ConsumoMaterial resultado = casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                consumo.getId(), new BigDecimal("50.00"), null));

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
                "Perda no setup"));

        assertThat(resultado.estaRegistrado()).isTrue();
        assertThat(resultado.getDesvio()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(resultado.getJustificativa()).isEqualTo("Perda no setup");
        assertThat(resultado.estaJustificado()).isTrue();
        // O responsável vem do login, não de um nome digitado
        assertThat(resultado.getJustificadoPor()).isEqualTo(USUARIO_TESTE);
        assertThat(trilha.eventos()).singleElement().satisfies(evento -> {
            assertThat(evento.getTipoEntidade()).isEqualTo(TipoEntidade.ORDEM_PRODUCAO);
            assertThat(evento.getEntidadeId()).isEqualTo(consumo.getOrdemProducaoId());
            assertThat(evento.getAcao()).isEqualTo(AcaoAuditoria.CONSUMO_REGISTRADO);
            assertThat(evento.getDetalhes())
                    .containsEntry("quantidadeConsumida", "55")
                    .containsEntry("desvio", "5")
                    .containsEntry("justificativa", "Perda no setup");
        });
    }

    @Test
    @DisplayName("rejeita registro de consumo não encontrado")
    void rejeitaConsumoNaoEncontrado() {
        assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                UUID.randomUUID(), new BigDecimal("10"), null)))
                .isInstanceOf(ConsumoMaterialNaoEncontradoException.class);
    }

    @Test
    @DisplayName("delega validação de desvio sem justificativa ao domínio")
    void delegaValidacaoDesvioAoDominio() {
        ConsumoMaterial consumo = criarConsumoProjetado(new BigDecimal("50.00"));
        repositorio.salvar(consumo);

        assertThatThrownBy(() -> casoDeUso.executar(new RegistrarConsumoMaterial.Comando(
                consumo.getId(), new BigDecimal("60.00"), null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Justificativa");
        assertThat(trilha.eventos()).isEmpty();
    }

    private ConsumoMaterial criarConsumoProjetado(BigDecimal qtdPlanejada) {
        return ConsumoMaterial.projetar(
                UUID.randomUUID(), UUID.randomUUID(), qtdPlanejada, "kg", USUARIO_TESTE);
    }
}
