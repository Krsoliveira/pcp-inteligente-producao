package com.krsoliveira.pcp.domain.lista;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NumeroDaListaTest {

    @ParameterizedTest(name = "{0} vira {1}")
    @CsvSource({"1,LT-0001", "42,LT-0042", "9999,LT-9999", "10000,LT-10000", "1234567,LT-1234567"})
    @DisplayName("tem ao menos 4 dígitos e cresce além do 9999 sem cortar nada")
    void formata(long numero, String esperado) {
        assertThat(NumeroDaLista.formatar(numero)).isEqualTo(esperado);
    }
}
