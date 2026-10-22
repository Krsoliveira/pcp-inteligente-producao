package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FiltroOrdensTest {

    @Test
    @DisplayName("busca em branco ou nula vira null (sem filtro)")
    void buscaEmBrancoViraNula() {
        assertThat(new FiltroOrdens("   ", null).busca()).isNull();
        assertThat(new FiltroOrdens(null, null).busca()).isNull();
    }

    @Test
    @DisplayName("busca é aparada nas pontas")
    void buscaEhAparada() {
        assertThat(new FiltroOrdens("  OP-2025  ", null).busca()).isEqualTo("OP-2025");
    }

    @Test
    @DisplayName("busca muito longa é cortada em 100 caracteres")
    void buscaLongaEhCortada() {
        assertThat(new FiltroOrdens("x".repeat(500), null).busca()).hasSize(100);
    }

    @Test
    @DisplayName("período invertido é rejeitado")
    void periodoInvertido() {
        assertThatThrownBy(() -> new FiltroOrdens(null, null, LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("fim do período");
    }

    @Test
    @DisplayName("período com um só limite ou com datas iguais é válido")
    void periodoValido() {
        assertThat(new FiltroOrdens(null, null, LocalDate.of(2026, 9, 1), null).de()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(new FiltroOrdens(null, null, null, LocalDate.of(2026, 9, 1)).ate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(new FiltroOrdens(null, null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1)).ate())
                .isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("semFiltro não tem busca, status nem período")
    void semFiltro() {
        var filtro = FiltroOrdens.semFiltro();

        assertThat(filtro.busca()).isNull();
        assertThat(filtro.status()).isNull();
        assertThat(filtro.de()).isNull();
        assertThat(filtro.ate()).isNull();
    }
}
