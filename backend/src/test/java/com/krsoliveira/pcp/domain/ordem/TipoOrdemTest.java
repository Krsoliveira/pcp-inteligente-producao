package com.krsoliveira.pcp.domain.ordem;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TipoOrdemTest {

    private static final String USUARIO = "teste@pcp";

    @Nested
    @DisplayName("Tipo de sistema Conversão")
    class Conversao {

        private TipoOrdem conversao() {
            return TipoOrdem.reconstituir(java.util.UUID.randomUUID(), "Conversão", "Acabado em semiacabado", "#6a1b9a", true,
                    new com.krsoliveira.pcp.domain.auditoria.Assinatura("sistema", java.time.Instant.now(), "sistema", java.time.Instant.now()));
        }

        @Test
        @DisplayName("tipos criados pelo usuário nunca são Conversão")
        void tiposDoUsuarioNaoSaoConversao() {
            assertThat(TipoOrdem.criar("Retrabalho", null, "#c62828", USUARIO).isConversao()).isFalse();
        }

        @Test
        @DisplayName("o tipo de sistema Conversão é reconhecido e não pode ser alterado")
        void conversaoNaoSeAltera() {
            TipoOrdem tipo = conversao();

            assertThat(tipo.isConversao()).isTrue();
            assertThatThrownBy(() -> tipo.atualizar("Outro nome", null, "#000000", USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("do sistema");
            assertThat(tipo.getNome()).isEqualTo("Conversão");
        }

        @Test
        @DisplayName("reconstituir sem a marca mantém o comportamento antigo (não é Conversão)")
        void reconstituirSemMarca() {
            TipoOrdem tipo = TipoOrdem.reconstituir(java.util.UUID.randomUUID(), "Normal", null, "#1565c0",
                    new com.krsoliveira.pcp.domain.auditoria.Assinatura("a", java.time.Instant.now(), "a", java.time.Instant.now()));

            assertThat(tipo.isConversao()).isFalse();
        }
    }

    @Nested
    @DisplayName("Criação")
    class Criacao {

        @Test
        @DisplayName("cria tipo de ordem válido")
        void criaTipoOrdemValido() {
            TipoOrdem tipo = TipoOrdem.criar("Produção Normal", "Ordens de produção padrão", "#1565c0", USUARIO);

            assertThat(tipo.getId()).isNotNull();
            assertThat(tipo.getNome()).isEqualTo("Produção Normal");
            assertThat(tipo.getDescricao()).isEqualTo("Ordens de produção padrão");
            assertThat(tipo.getCor()).isEqualTo("#1565c0");
            assertThat(tipo.getCriadoEm()).isNotNull();
        }

        @Test
        @DisplayName("aceita descrição nula")
        void aceitaDescricaoNula() {
            TipoOrdem tipo = TipoOrdem.criar("Manutenção", null, "#e65100", USUARIO);

            assertThat(tipo.getDescricao()).isNull();
        }

        @Test
        @DisplayName("rejeita nome em branco")
        void rejeitaNomeEmBranco() {
            assertThatThrownBy(() -> TipoOrdem.criar("  ", null, "#1565c0", USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nome");
        }

        @Test
        @DisplayName("rejeita cor em branco")
        void rejeitaCorEmBranco() {
            assertThatThrownBy(() -> TipoOrdem.criar("Teste", null, "  ", USUARIO))
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
            TipoOrdem tipo = TipoOrdem.criar("Produção", "Desc original", "#1565c0", USUARIO);
            var criadoEm = tipo.getCriadoEm();

            tipo.atualizar("Retrabalho", "Ordens de reprocessamento", "#d32f2f", USUARIO);

            assertThat(tipo.getNome()).isEqualTo("Retrabalho");
            assertThat(tipo.getDescricao()).isEqualTo("Ordens de reprocessamento");
            assertThat(tipo.getCor()).isEqualTo("#d32f2f");
            assertThat(tipo.getCriadoEm()).isEqualTo(criadoEm);
            assertThat(tipo.getAtualizadoEm()).isAfterOrEqualTo(criadoEm);
        }

        @Test
        @DisplayName("rejeita atualização com nome em branco")
        void rejeitaNomeEmBranco() {
            TipoOrdem tipo = TipoOrdem.criar("Produção", null, "#1565c0", USUARIO);

            assertThatThrownBy(() -> tipo.atualizar("", null, "#1565c0", USUARIO))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("nome");
        }
    }
}
