package com.krsoliveira.pcp.domain.ordem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("semFiltro não tem busca nem status")
    void semFiltro() {
        var filtro = FiltroOrdens.semFiltro();

        assertThat(filtro.busca()).isNull();
        assertThat(filtro.status()).isNull();
    }
}
