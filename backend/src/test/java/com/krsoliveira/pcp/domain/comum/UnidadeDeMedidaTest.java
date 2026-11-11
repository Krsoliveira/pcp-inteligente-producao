package com.krsoliveira.pcp.domain.comum;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnidadeDeMedidaTest {

    @ParameterizedTest(name = "\"{0}\" vira \"{1}\"")
    @CsvSource({"kg,KG", "Kg,KG", "KG,KG", "un,UN", "l,L", "m²,M²", "m³,M³", "kg/m3,KG/M3", "%,%", "cx-10,CX-10"})
    @DisplayName("vai para maiúsculas, de qualquer jeito que venha escrita")
    void normaliza(String entrada, String esperado) {
        assertThat(UnidadeDeMedida.normalizar(entrada)).isEqualTo(esperado);
    }

    @Test
    @DisplayName("tira os espaços das pontas")
    void tiraEspacosDasPontas() {
        assertThat(UnidadeDeMedida.normalizar("  kg  ")).isEqualTo("KG");
    }

    @ParameterizedTest(name = "\"{0}\" é recusada")
    @ValueSource(strings = {"", "   ", "k g", "kg;", "kg'", "<b>", "abcdefghijk", "k\ng"})
    @DisplayName("vazia, com espaço no meio, caractere fora do padrão ou mais de 10 caracteres é recusada")
    void recusa(String entrada) {
        assertThatThrownBy(() -> UnidadeDeMedida.normalizar(entrada))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("unidade de medida");
    }

    @Test
    @DisplayName("nula é recusada com a mesma mensagem")
    void recusaNula() {
        assertThatThrownBy(() -> UnidadeDeMedida.normalizar(null))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("unidade de medida");
    }

    @Test
    @DisplayName("aceita exatamente 10 caracteres")
    void aceitaDezCaracteres() {
        assertThat(UnidadeDeMedida.normalizar("abcdefghij")).isEqualTo("ABCDEFGHIJ");
    }
}
