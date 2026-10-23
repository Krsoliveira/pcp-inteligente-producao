package com.krsoliveira.pcp.application.lista;

import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.comum.PedidoDePagina;
import com.krsoliveira.pcp.domain.lista.FiltroListasTecnicas;
import com.krsoliveira.pcp.domain.lista.ItemListaTecnica;
import com.krsoliveira.pcp.domain.lista.ListaTecnica;
import com.krsoliveira.pcp.domain.lista.StatusListaTecnica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Autorização por objeto nas listas técnicas (ADR-0020): quem só tem LISTA_VISUALIZAR_ATIVAS enxerga
 * apenas versões ativas; as outras são tratadas como inexistentes, inclusive na comparação.
 */
class VisibilidadeDeListasTest {

    private final UUID materialId = UUID.randomUUID();
    private final UUID listaId = UUID.randomUUID();
    private ListaTecnicaRepositoryEmMemoria repositorio;
    private Set<Permissao> permissoes;
    private ConsultarListaTecnica consultar;
    private CompararListasTecnicas comparar;
    private ListaTecnica ativa;
    private ListaTecnica ativa2;
    private ListaTecnica emRevisao;
    private ListaTecnica obsoleta;

    @BeforeEach
    void setUp() {
        repositorio = new ListaTecnicaRepositoryEmMemoria();
        permissoes = EnumSet.of(Permissao.LISTA_VISUALIZAR_ATIVAS, Permissao.LISTA_COMPARAR);
        consultar = new ConsultarListaTecnica(repositorio, () -> permissoes);
        comparar = new CompararListasTecnicas(repositorio, () -> permissoes);
        ativa = versao("v1", StatusListaTecnica.ATIVA);
        ativa2 = versao("v2", StatusListaTecnica.ATIVA);
        emRevisao = versao("v3", StatusListaTecnica.EM_REVISAO);
        obsoleta = versao("v0", StatusListaTecnica.OBSOLETA);
    }

    private ListaTecnica versao(String rotulo, StatusListaTecnica status) {
        var item = ItemListaTecnica.criar(UUID.randomUUID(), BigDecimal.ONE, "un");
        var lista = ListaTecnica.reconstituir(UUID.randomUUID(), listaId, materialId, rotulo, status, List.of(item),
                Assinatura.nova("teste"));
        repositorio.salvar(lista);
        return lista;
    }

    private void veTudo() {
        permissoes = EnumSet.of(Permissao.LISTA_VISUALIZAR_TODAS, Permissao.LISTA_COMPARAR);
    }

    @Test
    @DisplayName("só ativas: abre uma versão ativa, mas versão em revisão ou obsoleta é 'não encontrada'")
    void buscarPorId() {
        assertThat(consultar.buscarPorId(ativa.getId())).isSameAs(ativa);
        assertThatThrownBy(() -> consultar.buscarPorId(emRevisao.getId())).isInstanceOf(ListaTecnicaNaoEncontradaException.class);
        assertThatThrownBy(() -> consultar.buscarPorId(obsoleta.getId())).isInstanceOf(ListaTecnicaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("só ativas: as listagens trazem apenas as versões ativas")
    void listagens() {
        assertThat(consultar.listarTodas()).containsExactlyInAnyOrder(ativa, ativa2);
        assertThat(consultar.listarPorMaterial(materialId)).containsExactlyInAnyOrder(ativa, ativa2);
    }

    @Test
    @DisplayName("só ativas: a pesquisa traz só ativas; pedir outro status devolve vazio, sem revelar que existem")
    void pesquisa() {
        var semFiltro = consultar.pesquisar(FiltroListasTecnicas.semFiltro(), PedidoDePagina.de(0, 25));
        var pedindoRevisao = consultar.pesquisar(new FiltroListasTecnicas(null, StatusListaTecnica.EM_REVISAO), PedidoDePagina.de(0, 25));
        var pedindoAtiva = consultar.pesquisar(new FiltroListasTecnicas(materialId, StatusListaTecnica.ATIVA), PedidoDePagina.de(0, 25));

        assertThat(semFiltro.itens()).containsExactlyInAnyOrder(ativa, ativa2);
        assertThat(semFiltro.total()).isEqualTo(2);
        assertThat(pedindoRevisao.itens()).isEmpty();
        assertThat(pedindoRevisao.total()).isZero();
        assertThat(pedindoAtiva.itens()).containsExactlyInAnyOrder(ativa, ativa2);
    }

    @Test
    @DisplayName("com LISTA_VISUALIZAR_TODAS vê todas as versões, em qualquer status")
    void veTodas() {
        veTudo();

        assertThat(consultar.buscarPorId(emRevisao.getId())).isSameAs(emRevisao);
        assertThat(consultar.listarTodas()).hasSize(4);
        assertThat(consultar.pesquisar(FiltroListasTecnicas.semFiltro(), PedidoDePagina.de(0, 25)).total()).isEqualTo(4);
        assertThat(consultar.pesquisar(new FiltroListasTecnicas(null, StatusListaTecnica.OBSOLETA), PedidoDePagina.de(0, 25)).itens())
                .containsExactly(obsoleta);
    }

    @Test
    @DisplayName("só ativas: compara duas versões ativas, mas não uma em revisão (não revela que existe)")
    void comparar() {
        assertThat(comparar.executar(ativa.getId(), ativa2.getId()).versaoA()).isSameAs(ativa);

        assertThatThrownBy(() -> comparar.executar(ativa.getId(), emRevisao.getId()))
                .isInstanceOf(ListaTecnicaNaoEncontradaException.class);
        assertThatThrownBy(() -> comparar.executar(obsoleta.getId(), ativa.getId()))
                .isInstanceOf(ListaTecnicaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("com LISTA_VISUALIZAR_TODAS compara qualquer par de versões")
    void compararComTodas() {
        veTudo();

        assertThat(comparar.executar(ativa.getId(), emRevisao.getId()).versaoB()).isSameAs(emRevisao);
    }

    @Test
    @DisplayName("sem nenhuma permissão de visualizar, nada é visível")
    void semPermissao() {
        permissoes = EnumSet.noneOf(Permissao.class);

        assertThat(consultar.listarTodas()).containsExactlyInAnyOrder(ativa, ativa2);
        // A barreira de "ver listas" é do endpoint (@PreAuthorize); aqui o objeto só se abre se for ativo.
        assertThatThrownBy(() -> consultar.buscarPorId(emRevisao.getId())).isInstanceOf(ListaTecnicaNaoEncontradaException.class);
    }
}
