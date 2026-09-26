package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoOrdemTest {

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("cria tipo de ordem válido")
        void criaTipoOrdemValido() {
            TipoOrdem tipo = TipoOrdem.criar("Produção Normal", "Ordens de produção padrão", "#1565c0");

            assertThat(tipo.getId()).isNotNull();
            assertThat(tipo.getNome()).isEqualTo("Produção Normal");
            assertThat(tipo.getDescricao()).isEqualTo("Ordens de produção padrão");
            assertThat(tipo.getCor()).isEqualTo("#1565c0");
            assertThat(tipo.getCriadoEm()).isNotNull();
        }

        @Test
        @DisplayName("aceita descrição nula")
        void aceitaDescricaoNula() {
            TipoOrdem tipo = TipoOrdem.criar("Manutenção", null, "#e65100");

            assertThat(tipo.getDescricao()).isNull();
        }

        @Test
        @DisplayName("rejeita nome em branco")
        void rejeitaNomeEmBranco() {
            assertThatThrownBy(() -> TipoOrdem.criar("  ", null, "#1565c0"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nome");
        }

        @Test
        @DisplayName("rejeita cor em branco")
        void rejeitaCorEmBranco() {
            assertThatThrownBy(() -> TipoOrdem.criar("Teste", null, "  "))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("cor");
        }
    }

    @Nested
    @DisplayName("Atualização")
    class Atualizacao {

        @Test
        @DisplayName("atualiza todos os campos editáveis")
        void atualizaCampos() {
            TipoOrdem tipo = TipoOrdem.criar("Produção", "Desc original", "#1565c0");
            var criadoEm = tipo.getCriadoEm();

            tipo.atualizar("Retrabalho", "Ordens de reprocessamento", "#d32f2f");

            assertThat(tipo.getNome()).isEqualTo("Retrabalho");
            assertThat(tipo.getDescricao()).isEqualTo("Ordens de reprocessamento");
            assertThat(tipo.getCor()).isEqualTo("#d32f2f");
            assertThat(tipo.getCriadoEm()).isEqualTo(criadoEm);
            assertThat(tipo.getAtualizadoEm()).isAfterOrEqualTo(criadoEm);
        }

        @Test
        @DisplayName("rejeita atualização com nome em branco")
        void rejeitaNomeEmBranco() {
            TipoOrdem tipo = TipoOrdem.criar("Produção", null, "#1565c0");

            assertThatThrownBy(() -> tipo.atualizar("", null, "#1565c0"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nome");
        }
    }
}
