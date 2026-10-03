package com.krsoliveira.pcp.domain.material;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodigoMaterialTest {

    @Test
    @DisplayName("primeiro e seguinte respeitam a faixa do tipo")
    void primeiroESeguinte() {
        assertThat(CodigoMaterial.primeiro(TipoMaterial.SEMIACABADO)).isEqualTo("105000001");
        assertThat(CodigoMaterial.seguinte("110000009", TipoMaterial.MATERIA_PRIMA)).isEqualTo("110000010");
    }

    @Test
    @DisplayName("faixa esgotada é rejeitada")
    void faixaEsgotada() {
        assertThatThrownBy(() -> CodigoMaterial.seguinte("103999999", TipoMaterial.PRODUTO_ACABADO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("esgotada");
    }

    @ParameterizedTest
    @ValueSource(strings = {"10300000", "1030000011", "103.000.001", "MAT-001", "10300000A"})
    @DisplayName("aceita só 9 dígitos numéricos")
    void rejeitaFormatoInvalido(String codigo) {
        assertThatThrownBy(() -> CodigoMaterial.validar(codigo, TipoMaterial.PRODUTO_ACABADO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("prefixo precisa corresponder ao tipo")
    void prefixoDoTipo() {
        assertThatCode(() -> CodigoMaterial.validar("110000001", TipoMaterial.MATERIA_PRIMA))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> CodigoMaterial.validar("110000001", TipoMaterial.PRODUTO_ACABADO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("faixa");
    }

    @Test
    @DisplayName("formata como 103.000.001")
    void formata() {
        assertThat(CodigoMaterial.formatar("103000001")).isEqualTo("103.000.001");
    }
}
