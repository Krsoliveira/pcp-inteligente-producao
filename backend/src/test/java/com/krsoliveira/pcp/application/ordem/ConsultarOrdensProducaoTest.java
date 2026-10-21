package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.auditoria.Pagina;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.ordem.FiltroOrdens;
import com.krsoliveira.pcp.domain.ordem.OrdemProducao;
import com.krsoliveira.pcp.domain.ordem.StatusOrdemProducao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarOrdensProducaoTest {

    private OrdemProducaoRepositoryEmMemoria repositorio;
    private ConsultarOrdensProducao casoDeUso;

    @BeforeEach
    void setUp() {
        repositorio = new OrdemProducaoRepositoryEmMemoria();
        casoDeUso = new ConsultarOrdensProducao(repositorio);
        for (int i = 1; i <= 7; i++) {
            repositorio.salvar(ordem("OP-A-00" + i));
        }
        var liberada = ordem("OP-B-001");
        liberada.alterarStatusPara(StatusOrdemProducao.LIBERADA, "teste");
        repositorio.salvar(liberada);
    }

    @Test
    @DisplayName("sem filtro devolve a primeira página e o total de todas as ordens")
    void semFiltro() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(FiltroOrdens.semFiltro(), PedidoDePagina.de(0, 5));

        assertThat(pagina.itens()).hasSize(5);
        assertThat(pagina.total()).isEqualTo(8);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("a última página traz só o que sobrou")
    void ultimaPagina() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(FiltroOrdens.semFiltro(), PedidoDePagina.de(1, 5));

        assertThat(pagina.itens()).hasSize(3);
        assertThat(pagina.pagina()).isEqualTo(1);
    }

    @Test
    @DisplayName("página além do fim volta vazia, mas com o total correto")
    void paginaAlemDoFim() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(FiltroOrdens.semFiltro(), PedidoDePagina.de(99, 5));

        assertThat(pagina.itens()).isEmpty();
        assertThat(pagina.total()).isEqualTo(8);
    }

    @Test
    @DisplayName("as ordens mais recentes vêm primeiro")
    void maisRecentesPrimeiro() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(FiltroOrdens.semFiltro(), PedidoDePagina.de(0, 3));

        assertThat(pagina.itens()).extracting(OrdemProducao::getCodigo)
                .containsExactly("OP-B-001", "OP-A-007", "OP-A-006");
    }

    @Test
    @DisplayName("busca pelo trecho do código, sem diferenciar maiúsculas")
    void buscaPorCodigo() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(new FiltroOrdens("op-b", null), PedidoDePagina.de(0, 25));

        assertThat(pagina.itens()).extracting(OrdemProducao::getCodigo).containsExactly("OP-B-001");
    }

    @Test
    @DisplayName("filtra por status")
    void filtroPorStatus() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(
                new FiltroOrdens(null, StatusOrdemProducao.LIBERADA), PedidoDePagina.de(0, 25));

        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("busca e status se combinam")
    void buscaEStatus() {
        Pagina<OrdemProducao> pagina = casoDeUso.pesquisar(
                new FiltroOrdens("OP-A", StatusOrdemProducao.LIBERADA), PedidoDePagina.de(0, 25));

        assertThat(pagina.total()).isZero();
    }

    private static OrdemProducao ordem(String codigo) {
        return OrdemProducao.criar(codigo, UUID.randomUUID(), UUID.randomUUID(), null, "Usinagem", 10,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 10), "teste");
    }
}
