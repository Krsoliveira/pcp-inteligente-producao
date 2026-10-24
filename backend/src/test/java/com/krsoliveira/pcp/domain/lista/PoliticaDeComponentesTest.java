package com.krsoliveira.pcp.domain.lista;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regra inegociável (ADR-0024): produto acabado só é consumido em ordem do tipo Conversão. */
class PoliticaDeComponentesTest {

    private static final Material ACABADO = material("103000001", TipoMaterial.PRODUTO_ACABADO);
    private static final Material OUTRO_ACABADO = material("103000002", TipoMaterial.PRODUTO_ACABADO);
    private static final Material SEMIACABADO = material("105000001", TipoMaterial.SEMIACABADO);
    private static final Material MATERIA_PRIMA = material("110000001", TipoMaterial.MATERIA_PRIMA);

    private static final TipoOrdem NORMAL = TipoOrdem.criar("Produção Normal", null, "#1565c0", "teste");
    private static final TipoOrdem CONVERSAO = TipoOrdem.reconstituir(UUID.randomUUID(), "Conversão", null, "#6a1b9a", true,
            new Assinatura("sistema", java.time.Instant.now(), "sistema", java.time.Instant.now()));

    private static Material material(String codigo, TipoMaterial tipo) {
        return Material.reconstituir(UUID.randomUUID(), codigo, "Material " + codigo, tipo, "un", Assinatura.nova("teste"));
    }

    @Test
    @DisplayName("lista de produto acabado aceita matéria-prima e semiacabado, mas não produto acabado")
    void listaDeProdutoAcabado() {
        assertThatCode(() -> PoliticaDeComponentes.validarLista(ACABADO, List.of(MATERIA_PRIMA, SEMIACABADO))).doesNotThrowAnyException();

        assertThatThrownBy(() -> PoliticaDeComponentes.validarLista(ACABADO, List.of(MATERIA_PRIMA, OUTRO_ACABADO)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("103000002")
                .hasMessageContaining("só listas de semiacabado");
    }

    @Test
    @DisplayName("lista de semiacabado pode ter produto acabado (a conversão)")
    void listaDeSemiacabado() {
        assertThatCode(() -> PoliticaDeComponentes.validarLista(SEMIACABADO, List.of(OUTRO_ACABADO, MATERIA_PRIMA))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ordem comum (sem tipo ou de outro tipo) não pode usar lista que consome produto acabado")
    void ordemComumNaoConsomeAcabado() {
        List<Material> comAcabado = List.of(MATERIA_PRIMA, OUTRO_ACABADO);

        assertThatThrownBy(() -> PoliticaDeComponentes.validarOrdem(SEMIACABADO, comAcabado, null))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("tipo Conversão");
        assertThatThrownBy(() -> PoliticaDeComponentes.validarOrdem(SEMIACABADO, comAcabado, NORMAL))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("tipo Conversão");
    }

    @Test
    @DisplayName("ordem do tipo Conversão pode consumir produto acabado ao produzir semiacabado")
    void conversaoConsomeAcabado() {
        assertThatCode(() -> PoliticaDeComponentes.validarOrdem(SEMIACABADO, List.of(OUTRO_ACABADO), CONVERSAO)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("o tipo Conversão só vale para ordens que produzem semiacabado")
    void conversaoSoProduzSemiacabado() {
        assertThatThrownBy(() -> PoliticaDeComponentes.validarOrdem(ACABADO, List.of(MATERIA_PRIMA), CONVERSAO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("produzem semiacabado");
    }

    @Test
    @DisplayName("item adicionado ao consumo: produto acabado só em ordem do tipo Conversão; os demais materiais em qualquer ordem")
    void itemAdicionado() {
        assertThatThrownBy(() -> PoliticaDeComponentes.validarItemAdicionado(ACABADO, null))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("tipo Conversão");
        assertThatThrownBy(() -> PoliticaDeComponentes.validarItemAdicionado(ACABADO, NORMAL))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("103000001");
        assertThatCode(() -> PoliticaDeComponentes.validarItemAdicionado(ACABADO, CONVERSAO)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaDeComponentes.validarItemAdicionado(MATERIA_PRIMA, null)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaDeComponentes.validarItemAdicionado(SEMIACABADO, NORMAL)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ordem comum sem produto acabado nos componentes continua livre")
    void ordemComumSemAcabado() {
        assertThatCode(() -> PoliticaDeComponentes.validarOrdem(ACABADO, List.of(MATERIA_PRIMA, SEMIACABADO), null)).doesNotThrowAnyException();
        assertThatCode(() -> PoliticaDeComponentes.validarOrdem(SEMIACABADO, List.of(MATERIA_PRIMA), NORMAL)).doesNotThrowAnyException();
        assertThat(PoliticaDeComponentes.consomeProdutoAcabado(List.of(MATERIA_PRIMA, SEMIACABADO))).isFalse();
        assertThat(PoliticaDeComponentes.consomeProdutoAcabado(List.of(MATERIA_PRIMA, ACABADO))).isTrue();
    }
}
