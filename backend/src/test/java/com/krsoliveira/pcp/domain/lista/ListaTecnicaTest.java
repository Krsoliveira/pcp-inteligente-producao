package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do domínio — JUnit puro, sem Spring, sem banco.
 */
class ListaTecnicaTest {

    private static final String USUARIO = "teste@pcp";

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID COMPONENTE_ID = UUID.randomUUID();

    private List<ItemListaTecnica> itensValidos() {
        return List.of(ItemListaTecnica.criar(COMPONENTE_ID, new BigDecimal("2.5"), "kg"));
    }

    private ListaTecnica listaValida() {
        return ListaTecnica.criar(MATERIAL_ID, "v1", itensValidos(), USUARIO);
    }

    @Nested
    @DisplayName("Quantidade base (ADR-0027)")
    class QuantidadeBase {

        private ListaTecnica comBase(String base) {
            return ListaTecnica.criar(MATERIAL_ID, "v1", new BigDecimal(base), itensValidos(), USUARIO);
        }

        @Test
        @DisplayName("sem informar, a base é 1: as listas continuam significando 'por 1 unidade'")
        void padraoEhUm() {
            assertThat(listaValida().getQuantidadeBase()).isEqualByComparingTo("1");
            assertThat(ListaTecnica.criarVersao(MATERIAL_ID, UUID.randomUUID(), "v2", itensValidos(), USUARIO).getQuantidadeBase())
                    .isEqualByComparingTo("1");
        }

        @Test
        @DisplayName("aceita uma base maior que 1, fracionária e com até 4 casas")
        void aceitaBases() {
            assertThat(comBase("100").getQuantidadeBase()).isEqualByComparingTo("100");
            assertThat(comBase("0.5").getQuantidadeBase()).isEqualByComparingTo("0.5");
            assertThat(comBase("2.5000").getQuantidadeBase()).isEqualByComparingTo("2.5");
            assertThat(comBase("0.0001").getQuantidadeBase()).isEqualByComparingTo("0.0001");
        }

        @Test
        @DisplayName("recusa base zero, negativa, nula, com mais de 4 casas ou de 100 milhões para cima")
        void recusaBasesInvalidas() {
            assertThatThrownBy(() -> comBase("0")).isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("maior que zero");
            assertThatThrownBy(() -> comBase("-1")).isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> ListaTecnica.criar(MATERIAL_ID, "v1", (BigDecimal) null, itensValidos(), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> comBase("1.00001")).isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("4 casas");
            assertThatThrownBy(() -> comBase("100000000")).isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("menor que");
        }

        @Test
        @DisplayName("quantidade necessária = quantidade do item ÷ base × quantidade da ordem")
        void quantidadeNecessaria() {
            ItemListaTecnica item = ItemListaTecnica.criar(COMPONENTE_ID, new BigDecimal("200"), "kg");

            assertThat(comBase("100").quantidadeNecessaria(item, 50)).isEqualByComparingTo("100"); // 200 kg por 100 un, ordem de 50
            assertThat(comBase("1").quantidadeNecessaria(item, 50)).isEqualByComparingTo("10000"); // a conta de sempre
            assertThat(comBase("100").quantidadeNecessaria(item, 1)).isEqualByComparingTo("2");
        }

        @Test
        @DisplayName("arredonda a quantidade necessária para 4 casas (meio para cima)")
        void arredonda() {
            ItemListaTecnica um = ItemListaTecnica.criar(COMPONENTE_ID, BigDecimal.ONE, "kg");

            assertThat(comBase("3").quantidadeNecessaria(um, 1)).isEqualByComparingTo("0.3333");
            assertThat(comBase("3").quantidadeNecessaria(um, 2)).isEqualByComparingTo("0.6667");
        }

        @Test
        @DisplayName("reconstituir mantém a base gravada, e sem informar assume 1")
        void reconstituir() {
            ListaTecnica base = comBase("100");

            ListaTecnica igual = ListaTecnica.reconstituir(base.getId(), base.getListaId(), MATERIAL_ID, "v1", new BigDecimal("100"),
                    base.getStatus(), itensValidos(), base.getAssinatura());
            ListaTecnica antiga = ListaTecnica.reconstituir(base.getId(), base.getListaId(), MATERIAL_ID, "v1",
                    base.getStatus(), itensValidos(), base.getAssinatura());

            assertThat(igual.getQuantidadeBase()).isEqualByComparingTo("100");
            assertThat(antiga.getQuantidadeBase()).isEqualByComparingTo("1");
        }
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("criar abre uma lista nova: o listaId é gerado e diferente do id da versão")
        void criarAbreListaNova() {
            ListaTecnica lista = listaValida();

            assertThat(lista.getListaId()).isNotNull().isNotEqualTo(lista.getId());
            assertThat(listaValida().getListaId()).isNotEqualTo(lista.getListaId());
        }

        @Test
        @DisplayName("criarVersao reaproveita o listaId informado")
        void criarVersaoReaproveitaListaId() {
            UUID listaId = UUID.randomUUID();

            ListaTecnica v2 = ListaTecnica.criarVersao(MATERIAL_ID, listaId, "v2", itensValidos(), USUARIO);

            assertThat(v2.getListaId()).isEqualTo(listaId);
            assertThat(v2.getStatus()).isEqualTo(StatusListaTecnica.EM_REVISAO);
        }

        @Test
        @DisplayName("criarVersao exige o listaId")
        void criarVersaoExigeListaId() {
            assertThatThrownBy(() -> ListaTecnica.criarVersao(MATERIAL_ID, null, "v2", itensValidos(), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("cria lista válida com status EM_REVISAO")
        void criaListaValida() {
            ListaTecnica lista = listaValida();

            assertThat(lista.getId()).isNotNull();
            assertThat(lista.getStatus()).isEqualTo(StatusListaTecnica.EM_REVISAO);
            assertThat(lista.getVersao()).isEqualTo("v1");
            assertThat(lista.getItens()).hasSize(1);
            assertThat(lista.getCriadaEm()).isNotNull();
        }

        @Test
        @DisplayName("rejeita lista sem itens")
        void rejeitaListaSemItens() {
            assertThatThrownBy(() -> ListaTecnica.criar(MATERIAL_ID, "v1", List.of(), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("componente");
        }

        @Test
        @DisplayName("rejeita versão em branco")
        void rejeitaVersaoEmBranco() {
            assertThatThrownBy(() -> ListaTecnica.criar(MATERIAL_ID, "  ", itensValidos(), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("versão");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() -> ListaTecnica.criar(null, "v1", itensValidos(), USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("material");
        }
    }

    @Nested
    @DisplayName("Ciclo de vida")
    class CicloDeVida {

        @Test
        @DisplayName("ativa lista em revisão")
        void ativaListaEmRevisao() {
            ListaTecnica lista = listaValida();

            lista.ativar(USUARIO);

            assertThat(lista.getStatus()).isEqualTo(StatusListaTecnica.ATIVA);
        }

        @Test
        @DisplayName("obsoleta lista ativa")
        void obseletaListaAtiva() {
            ListaTecnica lista = listaValida();
            lista.ativar(USUARIO);

            lista.obsoleter(USUARIO);

            assertThat(lista.getStatus()).isEqualTo(StatusListaTecnica.OBSOLETA);
        }

        @Test
        @DisplayName("não permite ativar lista já ativa")
        void naoPermiteAtivarListaJaAtiva() {
            ListaTecnica lista = listaValida();
            lista.ativar(USUARIO);

            assertThatThrownBy(() -> lista.ativar(USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("revisão");
        }

        @Test
        @DisplayName("não permite obsoletá-la sem ter ativado")
        void naoPermiteObsoletarEmRevisao() {
            ListaTecnica lista = listaValida();

            assertThatThrownBy(() -> lista.obsoleter(USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("ativas");
        }
    }

    @Nested
    @DisplayName("ItemListaTecnica")
    class ItemTeste {

        @Test
        @DisplayName("rejeita quantidade zero ou negativa")
        void rejeitaQuantidadeInvalida() {
            assertThatThrownBy(() ->
                    ItemListaTecnica.criar(COMPONENTE_ID, BigDecimal.ZERO, "kg"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("quantidade");
        }

        @Test
        @DisplayName("rejeita componente nulo")
        void rejeitaComponenteNulo() {
            assertThatThrownBy(() ->
                    ItemListaTecnica.criar(null, new BigDecimal("1"), "un"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("componente");
        }
    }
}
