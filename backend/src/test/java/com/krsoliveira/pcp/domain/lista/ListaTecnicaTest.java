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

    private static final UUID MATERIAL_ID = UUID.randomUUID();
    private static final UUID COMPONENTE_ID = UUID.randomUUID();

    private List<ItemListaTecnica> itensValidos() {
        return List.of(ItemListaTecnica.criar(COMPONENTE_ID, new BigDecimal("2.5"), "kg"));
    }

    private ListaTecnica listaValida() {
        return ListaTecnica.criar(MATERIAL_ID, "v1", itensValidos());
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

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
            assertThatThrownBy(() -> ListaTecnica.criar(MATERIAL_ID, "v1", List.of()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("componente");
        }

        @Test
        @DisplayName("rejeita versão em branco")
        void rejeitaVersaoEmBranco() {
            assertThatThrownBy(() -> ListaTecnica.criar(MATERIAL_ID, "  ", itensValidos()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("versão");
        }

        @Test
        @DisplayName("rejeita material nulo")
        void rejeitaMaterialNulo() {
            assertThatThrownBy(() -> ListaTecnica.criar(null, "v1", itensValidos()))
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

            lista.ativar();

            assertThat(lista.getStatus()).isEqualTo(StatusListaTecnica.ATIVA);
        }

        @Test
        @DisplayName("obsoleta lista ativa")
        void obseletaListaAtiva() {
            ListaTecnica lista = listaValida();
            lista.ativar();

            lista.obsoleter();

            assertThat(lista.getStatus()).isEqualTo(StatusListaTecnica.OBSOLETA);
        }

        @Test
        @DisplayName("não permite ativar lista já ativa")
        void naoPermiteAtivarListaJaAtiva() {
            ListaTecnica lista = listaValida();
            lista.ativar();

            assertThatThrownBy(lista::ativar)
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("revisão");
        }

        @Test
        @DisplayName("não permite obsoletá-la sem ter ativado")
        void naoPermiteObsoletarEmRevisao() {
            ListaTecnica lista = listaValida();

            assertThatThrownBy(lista::obsoleter)
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
