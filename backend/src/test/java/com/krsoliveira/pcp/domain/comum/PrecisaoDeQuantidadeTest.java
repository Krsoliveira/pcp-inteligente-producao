package com.krsoliveira.pcp.domain.comum;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrecisaoDeQuantidadeTest {

    @ParameterizedTest
    @ValueSource(strings = {"1", "1.5", "0.0001", "119.9088", "2.50000000", "100"})
    @DisplayName("aceita até 4 casas decimais, sem contar zeros à direita")
    void aceita(String valor) {
        assertThatCode(() -> PrecisaoDeQuantidade.exigir(new BigDecimal(valor), "A quantidade")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00001", "1.23456"})
    @DisplayName("recusa mais de 4 casas, em vez de deixar o banco arredondar em silêncio")
    void recusa(String valor) {
        assertThatThrownBy(() -> PrecisaoDeQuantidade.exigir(new BigDecimal(valor), "A quantidade"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("A quantidade aceita no máximo 4 casas decimais.");
    }

    @Test
    @DisplayName("valor ausente fica para a regra de obrigatoriedade de quem chama")
    void ausente() {
        assertThatCode(() -> PrecisaoDeQuantidade.exigir(null, "A quantidade")).doesNotThrowAnyException();
    }
}
