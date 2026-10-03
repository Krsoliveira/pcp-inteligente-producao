package com.krsoliveira.pcp.application.estoque;

import com.krsoliveira.pcp.application.material.MaterialRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.estoque.PosicaoEstoque;
import com.krsoliveira.pcp.domain.material.Material;
import com.krsoliveira.pcp.domain.material.TipoMaterial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultarEstoqueTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 3);

    @Test
    @DisplayName("lista todos os materiais por código; sem lote, a posição vem zerada")
    void todosOsMateriaisPorCodigo() {
        var materiais = new MaterialRepositoryEmMemoria();
        Material aco = Material.criar("110000001", "Aço", TipoMaterial.MATERIA_PRIMA, "kg", "t");
        Material viga = Material.criar("103000001", "Viga", TipoMaterial.PRODUTO_ACABADO, "un", "t");
        materiais.salvar(aco);
        materiais.salvar(viga);
        var posicaoAco = new PosicaoEstoque(aco.getId(), new BigDecimal("120"), 2, BigDecimal.ONE,
                HOJE.plusDays(30), null);

        var estoque = new ConsultarEstoque(hoje -> {
            assertThat(hoje).isEqualTo(HOJE);
            return List.of(posicaoAco);
        }, materiais).executar(HOJE);

        assertThat(estoque).extracting(i -> i.material().getCodigo()).containsExactly("103000001", "110000001");
        assertThat(estoque.get(0).posicao()).isEqualTo(PosicaoEstoque.vazia(viga.getId()));
        assertThat(estoque.get(1).posicao()).isEqualTo(posicaoAco);
    }
}
