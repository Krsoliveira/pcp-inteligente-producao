package com.krsoliveira.pcp.application.lote;

import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.lote.Lote;
import com.krsoliveira.pcp.domain.lote.OrigemCompra;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cenário: o lote de compra de aço (NF-1) é consumido pela ordem OP-EIXO, que gera o
 * lote de eixo. Do eixo chega-se à NF (origens); da NF chega-se ao eixo (destinos).
 */
class RastrearLoteTest {

    private static final String USUARIO = "teste@pcp";
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);
    private static final UUID ACO = UUID.randomUUID();
    private static final UUID EIXO = UUID.randomUUID();

    private final LoteRepositoryEmMemoria lotes = new LoteRepositoryEmMemoria();
    private final AlocacaoLoteRepositoryEmMemoria alocacoes = new AlocacaoLoteRepositoryEmMemoria();
    private final ConsumoMaterialRepositoryEmMemoria consumos = new ConsumoMaterialRepositoryEmMemoria();
    private final OrdemProducaoRepositoryEmMemoria ordens = new OrdemProducaoRepositoryEmMemoria();
    private final RastrearLote casoDeUso = new RastrearLote(lotes, alocacoes, consumos, ordens);

    private Lote loteAco;
    private Lote loteEixo;
    private OrdemProducao ordem;
    private ConsumoMaterial consumo;

    @BeforeEach
    void cenario() {
        loteAco = Lote.receberCompra("L-ACO", ACO,
                new OrigemCompra("Aços Brasil", "NF-1", HOJE.minusDays(10), HOJE.minusDays(9)), UUID.randomUUID(),
                new BigDecimal("100"), "kg", HOJE.minusDays(30), HOJE.plusYears(5), USUARIO);
        lotes.salvar(loteAco);

        ordem = OrdemProducao.criar("OP-EIXO", EIXO, UUID.randomUUID(), null, "CNC", 10,
                HOJE.minusDays(5), HOJE, USUARIO);
        ordem.alterarStatusPara(StatusOrdemProducao.LIBERADA, USUARIO);
        ordem.alterarStatusPara(StatusOrdemProducao.EM_PRODUCAO, USUARIO);
        consumo = ConsumoMaterial.projetar(ordem.getId(), ACO, new BigDecimal("26"), "kg", USUARIO);
        consumo.registrarConsumo(new BigDecimal("26"), null, USUARIO);
        alocacoes.salvarTodas(List.of(loteAco.alocar(consumo, new BigDecimal("26"), HOJE, USUARIO)));
        consumos.salvar(consumo);
        ordem.concluir(new BigDecimal("10"), USUARIO);
        ordens.salvar(ordem);

        loteEixo = Lote.criar("L-EIXO", EIXO, ordem.getId(), new BigDecimal("10"), "un",
                HOJE, HOJE.plusYears(1), USUARIO);
        lotes.salvar(loteEixo);
    }

    @Test
    @DisplayName("lote produzido: cada consumo mostra de quais lotes veio, até a nota fiscal")
    void origens() {
        RastrearLote.Resultado resultado = casoDeUso.executar(loteEixo.getId());

        assertThat(resultado.destinos()).isEmpty();
        assertThat(resultado.origens()).singleElement().satisfies(origem -> {
            assertThat(origem.consumo().getId()).isEqualTo(consumo.getId());
            assertThat(origem.lotes()).singleElement().satisfies(uso -> {
                assertThat(uso.lote().getNotaFiscal()).isEqualTo("NF-1");
                assertThat(uso.alocacao().getQuantidade()).isEqualByComparingTo("26");
            });
        });
    }

    @Test
    @DisplayName("lote de compra: mostra a ordem que o usou e o lote que ela gerou")
    void destinos() {
        RastrearLote.Resultado resultado = casoDeUso.executar(loteAco.getId());

        assertThat(resultado.origens()).isEmpty();
        assertThat(resultado.lote().getSaldo()).isEqualByComparingTo("74");
        assertThat(resultado.destinos()).singleElement().satisfies(destino -> {
            assertThat(destino.ordem().getCodigo()).isEqualTo("OP-EIXO");
            assertThat(destino.alocacao().getQuantidade()).isEqualByComparingTo("26");
            assertThat(destino.loteGerado()).hasValueSatisfying(l -> assertThat(l.getNumeroLote()).isEqualTo("L-EIXO"));
        });
    }

    @Test
    @DisplayName("lote inexistente gera não encontrado")
    void inexistente() {
        assertThatThrownBy(() -> casoDeUso.executar(UUID.randomUUID()))
                .isInstanceOf(LoteNaoEncontradoException.class);
    }
}
