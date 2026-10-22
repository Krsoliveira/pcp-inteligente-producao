package com.krsoliveira.pcp.domain.notafiscal;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiltroNotasFiscaisTest {

    @Test
    @DisplayName("busca em branco vira null, é aparada nas pontas e cortada em 100 caracteres")
    void normalizaABusca() {
        assertThat(new FiltroNotasFiscais("  ", null, null).busca()).isNull();
        assertThat(new FiltroNotasFiscais("  NF-1  ", null, null).busca()).isEqualTo("NF-1");
        assertThat(new FiltroNotasFiscais("x".repeat(300), null, null).busca()).hasSize(100);
    }

    @Test
    @DisplayName("aceita período com início igual ao fim e períodos abertos")
    void aceitaPeriodosValidos() {
        var dia = LocalDate.of(2026, 9, 1);

        assertThat(new FiltroNotasFiscais(null, dia, dia).de()).isEqualTo(dia);
        assertThat(new FiltroNotasFiscais(null, dia, null).ate()).isNull();
        assertThat(new FiltroNotasFiscais(null, null, dia).de()).isNull();
    }

    @Test
    @DisplayName("rejeita período com o fim antes do início")
    void rejeitaPeriodoInvertido() {
        assertThatThrownBy(() -> new FiltroNotasFiscais(null, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
