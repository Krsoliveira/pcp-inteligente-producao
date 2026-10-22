package com.krsoliveira.pcp.domain.material;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class FiltroMateriaisTest {

    @Test
    @DisplayName("busca em branco ou nula vira null e é aparada nas pontas")
    void normalizaABusca() {
        assertThat(new FiltroMateriais("   ", null).busca()).isNull();
        assertThat(new FiltroMateriais(null, null).busca()).isNull();
        assertThat(new FiltroMateriais("  aço  ", null).busca()).isEqualTo("aço");
    }

    @Test
    @DisplayName("busca muito longa é cortada em 100 caracteres")
    void cortaBuscaLonga() {
        assertThat(new FiltroMateriais("x".repeat(500), null).busca()).hasSize(100);
    }

    @ParameterizedTest(name = "\"{0}\" -> dígitos \"{1}\"")
    @CsvSource({
            "103.000.001, 103000001",
            "103000001, 103000001",
            "'103 000', 103000",
            "110., 110"
    })
    @DisplayName("código formatado vira só dígitos para procurar no código do material")
    void digitosDoCodigo(String busca, String esperado) {
        assertThat(new FiltroMateriais(busca, null).digitosDoCodigo()).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "\"{0}\" não é código")
    @CsvSource({"viga", "103.A00", "aço 3", "..."})
    @DisplayName("texto com letras, ou sem dígitos, não é tratado como código")
    void naoEhCodigo(String busca) {
        assertThat(new FiltroMateriais(busca, null).digitosDoCodigo()).isNull();
    }

    @Test
    @DisplayName("sem busca, não há dígitos de código")
    void semBusca() {
        assertThat(FiltroMateriais.semFiltro().digitosDoCodigo()).isNull();
    }

    @Test
    @DisplayName("aceita: busca na descrição sem diferenciar maiúsculas, no código formatado e no tipo")
    void aceitaMaterial() {
        var viga = Material.criar("103000001", "Viga metálica", TipoMaterial.PRODUTO_ACABADO, "un", "t");

        assertThat(FiltroMateriais.semFiltro().aceita(viga)).isTrue();
        assertThat(new FiltroMateriais("VIGA", null).aceita(viga)).isTrue();
        assertThat(new FiltroMateriais("103.000.001", null).aceita(viga)).isTrue();
        assertThat(new FiltroMateriais("000001", null).aceita(viga)).isTrue();
        assertThat(new FiltroMateriais(null, TipoMaterial.PRODUTO_ACABADO).aceita(viga)).isTrue();
    }

    @Test
    @DisplayName("aceita: recusa quando a busca ou o tipo não correspondem")
    void recusaMaterial() {
        var viga = Material.criar("103000001", "Viga metálica", TipoMaterial.PRODUTO_ACABADO, "un", "t");

        assertThat(new FiltroMateriais("chapa", null).aceita(viga)).isFalse();
        assertThat(new FiltroMateriais("999", null).aceita(viga)).isFalse();
        assertThat(new FiltroMateriais(null, TipoMaterial.MATERIA_PRIMA).aceita(viga)).isFalse();
        assertThat(new FiltroMateriais("viga", TipoMaterial.MATERIA_PRIMA).aceita(viga)).isFalse();
    }
}
