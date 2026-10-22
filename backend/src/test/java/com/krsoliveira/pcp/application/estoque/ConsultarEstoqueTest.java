package com.krsoliveira.pcp.application.estoque;

import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.estoque.FiltroEstoque;
import com.krsoliveira.pcp.domain.estoque.PosicaoEstoque;
import com.krsoliveira.pcp.domain.material.FiltroMateriais;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarEstoqueTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);

    @Test
    @DisplayName("lista todos os materiais por código; sem lote, a posição vem zerada")
    void todosOsMateriaisPorCodigo() {
        var materiais = new MaterialRepositoryEmMemoria();
        Material aco = Material.criar("110000001", "Aço", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        Material viga = Material.criar("103000001", "Viga", TipoMaterial.PRODUTO_ACABADO, "un", "t");
        materiais.salvar(aco);
        materiais.salvar(viga);
        var posicaoAco = new PosicaoEstoque(aco.getId(), new BigDecimal("120"), 2, BigDecimal.ONE,
                HOJE.plusDays(30), null);

        var estoque = new ConsultarEstoque(hoje -> {
            assertThat(hoje).isEqualTo(HOJE);
            return List.of(posicaoAco);
        }, materiais).executar(HOJE);

        assertThat(estoque).extracting(i -> i.material().getCodigo()).containsExactly("103000001", "110000001");
        assertThat(estoque.get(0).posicao()).isEqualTo(PosicaoEstoque.vazia(viga.getId()));
        assertThat(estoque.get(1).posicao()).isEqualTo(posicaoAco);
    }

    // ------------------------------------------------------------------ pesquisa e resumo

    private final MaterialRepositoryEmMemoria materiais = new MaterialRepositoryEmMemoria();
    private Material aco;
    private Material cobre;
    private Material zinco;
    private Material viga;

    /** Aço (entrada mais recente, vence em 10 dias), Cobre (entrada antiga, sem saldo), Zinco e Viga sem entrada. */
    private ConsultarEstoque estoqueDeExemplo() {
        aco = Material.criar("110000001", "Aço inox", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        cobre = Material.criar("110000002", "Cobre", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        zinco = Material.criar("110000003", "Zinco", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        viga = Material.criar("103000001", "Viga", TipoMaterial.PRODUTO_ACABADO, "un", "t");
        List.of(aco, cobre, zinco, viga).forEach(materiais::salvar);
        var posicoes = List.of(
                new PosicaoEstoque(aco.getId(), new BigDecimal("50"), 1, BigDecimal.ZERO, HOJE.plusDays(10),
                        Instant.parse("2026-10-02T10:00:00Z")),
                new PosicaoEstoque(cobre.getId(), BigDecimal.ZERO, 0, new BigDecimal("5"), null,
                        Instant.parse("2026-09-01T10:00:00Z")));
        return new ConsultarEstoque(hoje -> posicoes, materiais);
    }

    @Test
    @DisplayName("pesquisa: entrada mais recente primeiro; quem nunca teve entrada vem por último, por código")
    void pesquisaOrdenaPelaUltimaEntrada() {
        var pagina = estoqueDeExemplo().pesquisar(FiltroEstoque.semFiltro(), PedidoDePagina.de(0, 25), HOJE);

        assertThat(pagina.itens()).extracting(i -> i.material().getCodigo())
                .containsExactly("110000001", "110000002", "103000001", "110000003");
        assertThat(pagina.total()).isEqualTo(4);
    }

    @Test
    @DisplayName("pesquisa: pagina o resultado já filtrado")
    void pesquisaPagina() {
        var consulta = estoqueDeExemplo();

        var segunda = consulta.pesquisar(FiltroEstoque.semFiltro(), PedidoDePagina.de(1, 3), HOJE);

        assertThat(segunda.itens()).extracting(i -> i.material().getCodigo()).containsExactly("110000003");
        assertThat(segunda.total()).isEqualTo(4);
        assertThat(segunda.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("pesquisa: filtra por busca, tipo e só com saldo")
    void pesquisaFiltra() {
        var consulta = estoqueDeExemplo();

        var porTipo = consulta.pesquisar(new FiltroEstoque(new FiltroMateriais(null, TipoMaterial.PRODUTO_ACABADO), false),
                PedidoDePagina.de(0, 25), HOJE);
        var porBusca = consulta.pesquisar(new FiltroEstoque(new FiltroMateriais("110.000.002", null), false),
                PedidoDePagina.de(0, 25), HOJE);
        var comSaldo = consulta.pesquisar(new FiltroEstoque(FiltroMateriais.semFiltro(), true),
                PedidoDePagina.de(0, 25), HOJE);

        assertThat(porTipo.itens()).extracting(i -> i.material().getDescricao()).containsExactly("Viga");
        assertThat(porBusca.itens()).extracting(i -> i.material().getDescricao()).containsExactly("Cobre");
        assertThat(comSaldo.itens()).extracting(i -> i.material().getDescricao()).containsExactly("Aço inox");
    }

    @Test
    @DisplayName("resumo: conta materiais, os que vencem em até 30 dias e as matérias-primas sem saldo")
    void resumoDosAlertas() {
        var resumo = estoqueDeExemplo().resumo(HOJE);

        assertThat(resumo.totalMateriais()).isEqualTo(4);
        assertThat(resumo.materiaisVencendo()).isEqualTo(1);
        assertThat(resumo.materiasPrimasSemSaldo()).isEqualTo(2);
    }

    @Test
    @DisplayName("resumo: vencimento exatamente em 30 dias ainda entra no alerta; em 31 não")
    void resumoLimiteDeVencimento() {
        var materiaisLimite = new MaterialRepositoryEmMemoria();
        Material a = Material.criar("110000001", "A", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        Material b = Material.criar("110000002", "B", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        materiaisLimite.salvar(a);
        materiaisLimite.salvar(b);
        var posicoes = List.of(
                new PosicaoEstoque(a.getId(), BigDecimal.ONE, 1, BigDecimal.ZERO, HOJE.plusDays(30), null),
                new PosicaoEstoque(b.getId(), BigDecimal.ONE, 1, BigDecimal.ZERO, HOJE.plusDays(31), null));

        var resumo = new ConsultarEstoque(hoje -> posicoes, materiaisLimite).resumo(HOJE);

        assertThat(resumo.materiaisVencendo()).isEqualTo(1);
    }
}
