package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.auth.UsuarioRepositoryEmMemoria;
import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Trocar a própria senha e redefinir a de alguém (ADR-0021). */
class SenhasDeUsuarioTest {

    private static final CodificadorDeSenha CODIFICADOR = new CodificadorDeSenha() {
        @Override public String codificar(String senhaPlana) { return "hash:" + senhaPlana; }
        @Override public boolean verificar(String senhaPlana, String senhaHash) { return senhaHash.equals("hash:" + senhaPlana); }
    };

    private PerfilAcessoRepositoryEmMemoria perfis;
    private UsuarioRepositoryEmMemoria usuarios;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private Set<Permissao> doAtor;
    private TrocarMinhaSenha trocar;
    private RedefinirSenhaDoUsuario redefinir;

    @BeforeEach
    void setUp() {
        perfis = new PerfilAcessoRepositoryEmMemoria();
        usuarios = new UsuarioRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        doAtor = EnumSet.allOf(Permissao.class);
        trocar = new TrocarMinhaSenha(usuarios, CODIFICADOR, trilha.execucao());
        redefinir = new RedefinirSenhaDoUsuario(usuarios, perfis, CODIFICADOR, () -> doAtor, trilha.execucao());
    }

    /** Quem está logado nos testes é o {@code teste@pcp}. */
    private Usuario euComSenhaProvisoria() {
        return usuarios.salvar(Usuario.criarComSenhaProvisoria("Eu", USUARIO_TESTE, "hash:provisoria1", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR));
    }

    private Usuario outro(String email, UUID perfilId) {
        return usuarios.salvar(Usuario.criar("Outro", email, "hash:senhaAntiga1", perfilId));
    }

    @Nested
    @DisplayName("Trocar a própria senha")
    class Trocar {

        @Test
        @DisplayName("com a senha atual certa troca a senha, desliga a marca e registra SENHA_ALTERADA sem a senha")
        void trocaERegistra() {
            Usuario eu = euComSenhaProvisoria();

            trocar.executar("provisoria1", "minhaNovaSenha");

            Usuario salvo = usuarios.porId(eu.getId()).orElseThrow();
            assertThat(salvo.getSenhaHash()).isEqualTo("hash:minhaNovaSenha");
            assertThat(salvo.isDeveTrocarSenha()).isFalse();
            var evento = trilha.eventos(AcaoAuditoria.SENHA_ALTERADA).getFirst();
            assertThat(evento.getReferencia()).isEqualTo(USUARIO_TESTE);
            assertThat(evento.getDetalhes()).containsEntry("eraProvisoria", true);
            assertThat(evento.getDetalhes().toString()).doesNotContain("minhaNovaSenha").doesNotContain("provisoria1").doesNotContain("hash");
        }

        @Test
        @DisplayName("quem já tinha trocado também pode trocar de novo, e o evento diz que não era provisória")
        void trocaVoluntaria() {
            usuarios.salvar(Usuario.criar("Eu", USUARIO_TESTE, "hash:senhaAtual1", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR));

            trocar.executar("senhaAtual1", "outraSenha12");

            assertThat(trilha.eventos(AcaoAuditoria.SENHA_ALTERADA).getFirst().getDetalhes()).containsEntry("eraProvisoria", false);
        }

        @Test
        @DisplayName("senha atual errada é recusada (422) e nada muda")
        void atualErrada() {
            Usuario eu = euComSenhaProvisoria();

            assertThatThrownBy(() -> trocar.executar("errada123", "minhaNovaSenha"))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("senha atual não confere");

            assertThat(usuarios.porId(eu.getId()).orElseThrow().getSenhaHash()).isEqualTo("hash:provisoria1");
            assertThat(usuarios.porId(eu.getId()).orElseThrow().isDeveTrocarSenha()).isTrue();
            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("senha atual ausente é tratada como errada")
        void atualAusente() {
            euComSenhaProvisoria();

            assertThatThrownBy(() -> trocar.executar(null, "minhaNovaSenha")).isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        @DisplayName("nova senha curta, longa demais ou igual à atual é recusada")
        void novaSenhaInvalida() {
            Usuario eu = euComSenhaProvisoria();

            assertThatThrownBy(() -> trocar.executar("provisoria1", "curta"))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("mínimo 8");
            assertThatThrownBy(() -> trocar.executar("provisoria1", "a".repeat(73)))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("72");
            assertThatThrownBy(() -> trocar.executar("provisoria1", "provisoria1"))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("diferente da atual");

            assertThat(usuarios.porId(eu.getId()).orElseThrow().isDeveTrocarSenha()).isTrue();
            assertThat(trilha.eventos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Redefinir a senha de alguém")
    class Redefinir {

        @Test
        @DisplayName("troca a senha, liga a marca de troca obrigatória e registra SENHA_REDEFINIDA sem a senha")
        void redefineERegistra() {
            Usuario ana = outro("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            redefinir.executar(ana.getId(), "novaProvisoria1");

            Usuario salvo = usuarios.porId(ana.getId()).orElseThrow();
            assertThat(salvo.getSenhaHash()).isEqualTo("hash:novaProvisoria1");
            assertThat(salvo.isDeveTrocarSenha()).isTrue();
            var evento = trilha.eventos(AcaoAuditoria.SENHA_REDEFINIDA).getFirst();
            assertThat(evento.getReferencia()).isEqualTo("ana@pcp.com");
            assertThat(evento.getUsuario()).isEqualTo(USUARIO_TESTE);
            assertThat(evento.getDetalhes().toString()).doesNotContain("novaProvisoria1").doesNotContain("hash");
        }

        @Test
        @DisplayName("ninguém redefine a própria senha por aqui")
        void naoRedefineAPropria() {
            Usuario eu = usuarios.salvar(Usuario.criar("Eu", USUARIO_TESTE, "hash:senhaAtual1", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR));

            assertThatThrownBy(() -> redefinir.executar(eu.getId(), "novaProvisoria1"))
                    .isInstanceOf(AcessoNegadoException.class).hasMessageContaining("Alterar minha senha");

            assertThat(usuarios.porId(eu.getId()).orElseThrow().getSenhaHash()).isEqualTo("hash:senhaAtual1");
        }

        @Test
        @DisplayName("um gestor não redefine a senha de quem tem perfil acima do alcance dele (senão assumiria a conta)")
        void naoAssumeConta() {
            Usuario chefe = outro("chefe@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);
            doAtor = EnumSet.of(Permissao.USUARIO_GERENCIAR, Permissao.ACESSO_VISUALIZAR, Permissao.ORDEM_VISUALIZAR);

            assertThatThrownBy(() -> redefinir.executar(chefe.getId(), "novaProvisoria1")).isInstanceOf(AcessoNegadoException.class);

            assertThat(usuarios.porId(chefe.getId()).orElseThrow().getSenhaHash()).isEqualTo("hash:senhaAntiga1");
            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("um gestor redefine a senha de quem tem perfil igual ou menor que o dele")
        void alcanca() {
            PerfilAcesso operador = perfis.salvar(PerfilAcesso.criar("Operador", "", List.of(Permissao.ORDEM_VISUALIZAR), "sistema"));
            Usuario bia = outro("bia@pcp.com", operador.getId());
            doAtor = EnumSet.of(Permissao.USUARIO_GERENCIAR, Permissao.ORDEM_VISUALIZAR);

            redefinir.executar(bia.getId(), "novaProvisoria1");

            assertThat(usuarios.porId(bia.getId()).orElseThrow().isDeveTrocarSenha()).isTrue();
        }

        @Test
        @DisplayName("senha provisória fora da regra e usuário inexistente são recusados")
        void invalidos() {
            Usuario ana = outro("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            assertThatThrownBy(() -> redefinir.executar(ana.getId(), "curta")).isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> redefinir.executar(UUID.randomUUID(), "novaProvisoria1"))
                    .isInstanceOf(UsuarioNaoEncontradoException.class);

            assertThat(usuarios.porId(ana.getId()).orElseThrow().getSenhaHash()).isEqualTo("hash:senhaAntiga1");
        }
    }
}
