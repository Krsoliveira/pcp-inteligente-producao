package com.krsoliveira.pcp.domain.material;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários do domínio — JUnit puro, sem Spring, sem banco.
 */
class MaterialTest {

    private Material materialValido() {
        return Material.criar("MAT-001", "Motor elétrico 5CV", TipoMaterial.PRODUTO_ACABADO, "un");
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("cria material válido e normaliza código em maiúsculas")
        void criaMaterialValido() {
            Material material = Material.criar("mat-001", "Motor elétrico 5CV",
                    TipoMaterial.PRODUTO_ACABADO, "un");

            assertThat(material.getId()).isNotNull();
            assertThat(material.getCodigo()).isEqualTo("MAT-001");
            assertThat(material.getTipo()).isEqualTo(TipoMaterial.PRODUTO_ACABADO);
            assertThat(material.getCriadoEm()).isNotNull();
            assertThat(material.getAtualizadoEm()).isNotNull();
        }

        @Test
        @DisplayName("rejeita código em branco")
        void rejeitaCodigoEmBranco() {
            assertThatThrownBy(() ->
                    Material.criar("  ", "Descrição", TipoMaterial.MATERIA_PRIMA, "kg"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("código");
        }

        @Test
        @DisplayName("rejeita descrição em branco")
        void rejeitaDescricaoEmBranco() {
            assertThatThrownBy(() ->
                    Material.criar("MAT-001", "", TipoMaterial.SEMIACABADO, "m"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("descrição");
        }

        @Test
        @DisplayName("rejeita tipo nulo")
        void rejeitaTipoNulo() {
            assertThatThrownBy(() ->
                    Material.criar("MAT-001", "Descrição", null, "un"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("tipo");
        }

        @Test
        @DisplayName("rejeita unidade de medida em branco")
        void rejeitaUnidadeEmBranco() {
            assertThatThrownBy(() ->
                    Material.criar("MAT-001", "Descrição", TipoMaterial.PRODUTO_ACABADO, "  "))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("unidade");
        }
    }

    @Nested
    @DisplayName("Tipo e regras de BOM")
    class TipoERegras {

        @Test
        @DisplayName("PRODUTO_ACABADO e SEMIACABADO podem ter lista técnica")
        void produtoAcabadoEsemiacabadoPodeTerListaTecnica() {
            assertThat(TipoMaterial.PRODUTO_ACABADO.podeTermListaTecnica()).isTrue();
            assertThat(TipoMaterial.SEMIACABADO.podeTermListaTecnica()).isTrue();
        }

        @Test
        @DisplayName("MATERIA_PRIMA não pode ter lista técnica")
        void materiaPrimaNaoPodeTerListaTecnica() {
            assertThat(TipoMaterial.MATERIA_PRIMA.podeTermListaTecnica()).isFalse();
        }
    }
}
