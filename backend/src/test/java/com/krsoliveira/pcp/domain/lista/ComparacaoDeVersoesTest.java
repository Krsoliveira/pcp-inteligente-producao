package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComparacaoDeVersoesTest {

    private static final UUID MATERIAL = UUID.randomUUID();
    private static final UUID LISTA = UUID.randomUUID();
    private static final UUID ACO = UUID.randomUUID();
    private static final UUID TINTA = UUID.randomUUID();
    private static final UUID PARAFUSO = UUID.randomUUID();
    private static final UUID CAIXA = UUID.randomUUID();

    private static ListaTecnica versao(String rotulo, ItemListaTecnica... itens) {
        return ListaTecnica.criarVersao(MATERIAL, LISTA, rotulo, List.of(itens), "teste");
    }

    private static ItemListaTecnica item(UUID componente, String quantidade, String unidade) {
        return ItemListaTecnica.criar(componente, new BigDecimal(quantidade), unidade);
    }

    @Test
    @DisplayName("bases diferentes: 2 kg para 1 un é o mesmo que 200 kg para 100 un (igual); 250 kg para 100 un é alteração")
    void comparaPorUnidadeProduzida() {
        ListaTecnica porUnidade = ListaTecnica.criarVersao(MATERIAL, LISTA, "v1", new BigDecimal("1"),
                List.of(item(ACO, "2", "kg"), item(PARAFUSO, "8", "un")), "teste");
        ListaTecnica porCem = ListaTecnica.criarVersao(MATERIAL, LISTA, "v2", new BigDecimal("100"),
                List.of(item(ACO, "200", "kg"), item(PARAFUSO, "850", "un")), "teste");

        ComparacaoDeVersoes comparacao = ComparacaoDeVersoes.de(porUnidade, porCem);

        assertThat(comparacao.iguais()).extracting(ComparacaoDeVersoes.ItemComparado::materialComponenteId).containsExactly(ACO);
        assertThat(comparacao.alterados()).extracting(ComparacaoDeVersoes.ItemComparado::materialComponenteId).containsExactly(PARAFUSO);
        assertThat(comparacao.alterados().get(0).quantidadeB()).isEqualByComparingTo("850"); // mostra o valor da própria versão
        assertThat(comparacao.temDiferencas()).isTrue();
    }

    @Test
    @DisplayName("separa componentes adicionados, removidos, alterados e iguais")
    void classificaAsDiferencas() {
        ListaTecnica v1 = versao("v1", item(ACO, "2.5", "kg"), item(TINTA, "1", "l"), item(PARAFUSO, "8", "un"));
        ListaTecnica v2 = versao("v2", item(ACO, "3", "kg"), item(PARAFUSO, "8", "un"), item(CAIXA, "1", "un"));

        ComparacaoDeVersoes comparacao = ComparacaoDeVersoes.de(v1, v2);

        assertThat(comparacao.adicionados()).extracting(ComparacaoDeVersoes.ItemComparado::materialComponenteId)
                .containsExactly(CAIXA);
        assertThat(comparacao.removidos()).extracting(ComparacaoDeVersoes.ItemComparado::materialComponenteId)
                .containsExactly(TINTA);
        assertThat(comparacao.alterados()).singleElement().satisfies(i -> {
            assertThat(i.materialComponenteId()).isEqualTo(ACO);
            assertThat(i.quantidadeA()).isEqualByComparingTo("2.5");
            assertThat(i.quantidadeB()).isEqualByComparingTo("3");
        });
        assertThat(comparacao.iguais()).extracting(ComparacaoDeVersoes.ItemComparado::materialComponenteId)
                .containsExactly(PARAFUSO);
        assertThat(comparacao.temDiferencas()).isTrue();
    }

    @Test
    @DisplayName("o lado em que o componente não existe fica com quantidade e unidade nulas")
    void ladoAusenteFicaNulo() {
        ComparacaoDeVersoes comparacao = ComparacaoDeVersoes.de(
                versao("v1", item(TINTA, "1", "l")), versao("v2", item(CAIXA, "1", "un")));

        assertThat(comparacao.removidos().getFirst().quantidadeB()).isNull();
        assertThat(comparacao.removidos().getFirst().unidadeB()).isNull();
        assertThat(comparacao.adicionados().getFirst().quantidadeA()).isNull();
        assertThat(comparacao.adicionados().getFirst().unidadeA()).isNull();
    }

    @Test
    @DisplayName("2.50 e 2.5 são a mesma quantidade; só a unidade diferente já conta como alteração")
    void quantidadeEUnidade() {
        ComparacaoDeVersoes mesmaQuantidade = ComparacaoDeVersoes.de(
                versao("v1", item(ACO, "2.50", "kg")), versao("v2", item(ACO, "2.5", "kg")));
        ComparacaoDeVersoes outraUnidade = ComparacaoDeVersoes.de(
                versao("v1", item(ACO, "2.5", "kg")), versao("v2", item(ACO, "2.5", "g")));

        assertThat(mesmaQuantidade.iguais()).hasSize(1);
        assertThat(mesmaQuantidade.temDiferencas()).isFalse();
        assertThat(outraUnidade.alterados()).hasSize(1);
    }

    @Test
    @DisplayName("versões idênticas não têm diferenças")
    void identicas() {
        ComparacaoDeVersoes comparacao = ComparacaoDeVersoes.de(
                versao("v1", item(ACO, "2", "kg")), versao("v2", item(ACO, "2", "kg")));

        assertThat(comparacao.temDiferencas()).isFalse();
    }

    @Test
    @DisplayName("rejeita comparar versões de listas diferentes")
    void rejeitaListasDiferentes() {
        ListaTecnica deOutraLista = ListaTecnica.criar(UUID.randomUUID(), "v1", List.of(item(ACO, "1", "kg")), "teste");

        assertThatThrownBy(() -> ComparacaoDeVersoes.de(versao("v1", item(ACO, "1", "kg")), deOutraLista))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("mesma lista");
    }

    @Test
    @DisplayName("rejeita comparar uma versão com ela mesma")
    void rejeitaMesmaVersao() {
        ListaTecnica v1 = versao("v1", item(ACO, "1", "kg"));
        ListaTecnica mesma = ListaTecnica.reconstituir(v1.getId(), v1.getListaId(), v1.getMaterialId(), "v1",
                v1.getStatus(), v1.getItens(), Assinatura.nova("teste"));

        assertThatThrownBy(() -> ComparacaoDeVersoes.de(v1, mesma))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("diferentes");
    }
}
