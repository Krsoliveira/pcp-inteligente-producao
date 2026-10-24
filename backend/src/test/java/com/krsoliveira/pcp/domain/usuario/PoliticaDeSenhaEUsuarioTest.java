package com.krsoliveira.pcp.domain.usuario;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaDeSenhaEUsuarioTest {

    @Nested
    @DisplayName("Política de senha")
    class Politica {

        @Test
        @DisplayName("aceita de 8 caracteres até 72 bytes, sem exigir complexidade")
        void aceita() {
            assertThatCode(() -> PoliticaDeSenha.validar("12345678")).doesNotThrowAnyException();
            assertThatCode(() -> PoliticaDeSenha.validar("a".repeat(72))).doesNotThrowAnyException();
            assertThatCode(() -> PoliticaDeSenha.validar("senha simples")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("recusa senha nula ou com menos de 8 caracteres")
        void recusaCurta() {
            assertThatThrownBy(() -> PoliticaDeSenha.validar(null)).isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> PoliticaDeSenha.validar("1234567"))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("mínimo 8");
        }

        @Test
        @DisplayName("recusa mais de 72 bytes (limite do BCrypt), contando bytes e não caracteres")
        void recusaLongaDemais() {
            assertThatThrownBy(() -> PoliticaDeSenha.validar("a".repeat(73)))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("72");
            // 37 caracteres acentuados = 74 bytes em UTF-8
            assertThatThrownBy(() -> PoliticaDeSenha.validar("é".repeat(37))).isInstanceOf(RegraDeNegocioException.class);
            assertThatCode(() -> PoliticaDeSenha.validar("é".repeat(36))).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Usuário e a troca de senha")
    class Usuarios {

        private final UUID perfil = UUID.randomUUID();

        @Test
        @DisplayName("usuário comum nasce sem troca obrigatória; o criado por gestor nasce com ela")
        void marcaDeTroca() {
            assertThat(Usuario.criar("Ana", "ana@pcp.com", "hash", perfil).isDeveTrocarSenha()).isFalse();
            assertThat(Usuario.criarComSenhaProvisoria("Ana", "ana@pcp.com", "hash", perfil).isDeveTrocarSenha()).isTrue();
        }

        @Test
        @DisplayName("trocar a senha desliga a marca e guarda o hash novo")
        void trocar() {
            Usuario usuario = Usuario.criarComSenhaProvisoria("Ana", "ana@pcp.com", "hash-provisorio", perfil);

            usuario.trocarSenha("hash-novo");

            assertThat(usuario.isDeveTrocarSenha()).isFalse();
            assertThat(usuario.getSenhaHash()).isEqualTo("hash-novo");
        }

        @Test
        @DisplayName("redefinir liga a marca de novo, mesmo para quem já tinha trocado")
        void redefinir() {
            Usuario usuario = Usuario.criar("Ana", "ana@pcp.com", "hash", perfil);

            usuario.redefinirSenha("hash-provisorio");

            assertThat(usuario.isDeveTrocarSenha()).isTrue();
            assertThat(usuario.getSenhaHash()).isEqualTo("hash-provisorio");
        }

        @Test
        @DisplayName("recusa hash vazio e não altera o usuário")
        void hashVazio() {
            Usuario usuario = Usuario.criarComSenhaProvisoria("Ana", "ana@pcp.com", "hash", perfil);

            assertThatThrownBy(() -> usuario.trocarSenha(" ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> usuario.redefinirSenha(null)).isInstanceOf(IllegalArgumentException.class);

            assertThat(usuario.getSenhaHash()).isEqualTo("hash");
            assertThat(usuario.isDeveTrocarSenha()).isTrue();
        }
    }
}
