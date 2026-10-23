package com.krsoliveira.pcp.application.relatorio;

import com.krsoliveira.pcp.application.consumo.ConsumoMaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.application.ordem.OrdemProducaoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.consumo.ConsumoMaterial;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Relatório de consumo de ordens: uma linha por ordem concluída e componente, com o consumo total. */
class ConsultarConsumoDeOrdensTest {

    private static final LocalDate DE = LocalDate.of(2026, 9, 1);
    private static final LocalDate ATE = LocalDate.of(2026, 9, 30);

    private OrdemProducaoRepositoryEmMemoria ordens;
    private ConsumoMaterialRepositoryEmMemoria consumos;
    private MaterialRepositoryEmMemoria materiais;
    private ConsultarConsumoDeOrdens casoDeUso;

    private Material produto;
    private Material parafuso;
    private Material chapa;

    @BeforeEach
    void setUp() {
        ordens = new OrdemProducaoRepositoryEmMemoria();
        consumos = new ConsumoMaterialRepositoryEmMemoria();
        materiais = new MaterialRepositoryEmMemoria();
        casoDeUso = new ConsultarConsumoDeOrdens(ordens, consumos, materiais);
        produto = material("103000001", "Redutor", TipoMaterial.PRODUTO_ACABADO, "un");
        parafuso = material("110000007", "Parafuso sextavado", TipoMaterial.MATERIA_PRIMA, "un");
        chapa = material("110000002", "Chapa de aço", TipoMaterial.MATERIA_PRIMA, "kg");
    }

    private Material material(String codigo, String descricao, TipoMaterial tipo, String unidade) {
        Material material = Material.criar(codigo, descricao, tipo, unidade, "teste");
        materiais.salvar(material);
        return material;
    }

    private OrdemProducao ordem(String codigo, StatusOrdemProducao status, LocalDate fim) {
        Assinatura assinatura = new Assinatura("ana", Instant.parse("2026-01-01T12:00:00Z"), "ana", Instant.parse("2026-09-05T12:00:00Z"));
        return ordens.salvar(OrdemProducao.reconstituir(UUID.randomUUID(), codigo, produto.getId(), UUID.randomUUID(), null,
                "Usinagem", 10, BigDecimal.TEN, fim.minusDays(5), fim, status, assinatura));
    }

    private void consumo(OrdemProducao ordem, Material componente, String planejado, String consumido) {
        consumos.salvar(ConsumoMaterial.reconstituir(UUID.randomUUID(), ordem.getId(), componente.getId(),
                new BigDecimal(planejado), consumido == null ? null : new BigDecimal(consumido), componente.getUnidadeDeMedida(),
                null, null, null, new Assinatura("ana", Instant.now(), "ana", Instant.now())));
    }

    @Test
    @DisplayName("repete a ordem em cada linha de componente, com o consumo total e o desvio")
    void umaLinhaPorOrdemEComponente() {
        OrdemProducao op = ordem("OP-1", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10));
        consumo(op, parafuso, "40", "42");
        consumo(op, chapa, "5.5", "5.5");

        RelatorioConsumoDeOrdens relatorio = casoDeUso.executar(DE, ATE);

        assertThat(relatorio.linhas()).hasSize(2);
        assertThat(relatorio.linhas()).extracting(LinhaDeConsumoDeOrdem::ordemCodigo).containsOnly("OP-1");
        LinhaDeConsumoDeOrdem linhaDoParafuso = relatorio.linhas().stream()
                .filter(l -> l.componenteCodigo().equals("110000007")).findFirst().orElseThrow();
        assertThat(linhaDoParafuso.componenteDescricao()).isEqualTo("Parafuso sextavado");
        assertThat(linhaDoParafuso.unidadeDeMedida()).isEqualTo("un");
        assertThat(linhaDoParafuso.quantidadePlanejada()).isEqualByComparingTo("40");
        assertThat(linhaDoParafuso.quantidadeConsumida()).isEqualByComparingTo("42");
        assertThat(linhaDoParafuso.desvio()).isEqualByComparingTo("2");
        assertThat(linhaDoParafuso.produtoCodigo()).isEqualTo("103000001");
        assertThat(relatorio.totalDeOrdens()).isEqualTo(1);
    }

    @Test
    @DisplayName("ordena por fim planejado, ordem e código do componente")
    void ordenacao() {
        OrdemProducao tardia = ordem("OP-2", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 20));
        OrdemProducao cedo = ordem("OP-1", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10));
        consumo(tardia, parafuso, "1", "1");
        consumo(cedo, parafuso, "1", "1");
        consumo(cedo, chapa, "1", "1");

        RelatorioConsumoDeOrdens relatorio = casoDeUso.executar(DE, ATE);

        assertThat(relatorio.linhas()).extracting(l -> l.ordemCodigo() + "/" + l.componenteCodigo())
                .containsExactly("OP-1/110000002", "OP-1/110000007", "OP-2/110000007");
        assertThat(relatorio.totalDeOrdens()).isEqualTo(2);
    }

    @Test
    @DisplayName("só entram ordens concluídas com fim planejado no período (limites inclusivos); abertas e canceladas ficam de fora")
    void soConcluidasNoPeriodo() {
        consumo(ordem("OP-NO-INICIO", StatusOrdemProducao.CONCLUIDA, DE), parafuso, "1", "1");
        consumo(ordem("OP-NO-FIM", StatusOrdemProducao.CONCLUIDA, ATE), parafuso, "1", "1");
        consumo(ordem("OP-ANTES", StatusOrdemProducao.CONCLUIDA, DE.minusDays(1)), parafuso, "1", "1");
        consumo(ordem("OP-DEPOIS", StatusOrdemProducao.CONCLUIDA, ATE.plusDays(1)), parafuso, "1", "1");
        consumo(ordem("OP-ABERTA", StatusOrdemProducao.EM_PRODUCAO, LocalDate.of(2026, 9, 15)), parafuso, "1", null);
        consumo(ordem("OP-CANCELADA", StatusOrdemProducao.CANCELADA, LocalDate.of(2026, 9, 15)), parafuso, "1", null);

        assertThat(casoDeUso.executar(DE, ATE).linhas()).extracting(LinhaDeConsumoDeOrdem::ordemCodigo)
                .containsExactly("OP-NO-INICIO", "OP-NO-FIM");
    }

    @Test
    @DisplayName("período sem ordens concluídas devolve relatório vazio, não erro")
    void periodoSemOrdens() {
        RelatorioConsumoDeOrdens relatorio = casoDeUso.executar(DE, ATE);

        assertThat(relatorio.linhas()).isEmpty();
        assertThat(relatorio.totalDeOrdens()).isZero();
        assertThat(relatorio.de()).isEqualTo(DE);
        assertThat(relatorio.ate()).isEqualTo(ATE);
    }

    @Test
    @DisplayName("período obrigatório, em ordem e de no máximo 366 dias")
    void validaOPeriodo() {
        assertThatThrownBy(() -> casoDeUso.executar(null, ATE)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> casoDeUso.executar(DE, null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> casoDeUso.executar(ATE, DE)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("anterior");
        assertThatThrownBy(() -> casoDeUso.executar(DE, DE.plusDays(366))).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("366");
        assertThat(casoDeUso.executar(DE, DE.plusDays(365)).linhas()).isEmpty(); // exatamente 366 dias, inclusive
    }

    @Test
    @DisplayName("passando do limite de linhas pede para reduzir o período em vez de devolver um relatório gigante")
    void limiteDeLinhas() {
        OrdemProducao op = ordem("OP-1", StatusOrdemProducao.CONCLUIDA, LocalDate.of(2026, 9, 10));
        for (int i = 0; i < ConsultarConsumoDeOrdens.MAXIMO_DE_LINHAS + 1; i++) {
            consumo(op, parafuso, "1", "1");
        }

        assertThatThrownBy(() -> casoDeUso.executar(DE, ATE)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("reduza o período");
    }
}
