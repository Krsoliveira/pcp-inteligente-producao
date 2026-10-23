package com.krsoliveira.pcp.application.auth;

import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.application.acesso.PerfilAcessoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistrarUsuarioTest {

    /** Codificador de senha trivial: devolve a senha em claro prefixada para identificação nos asserts. */
    private static final com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha CODIFICADOR_TRIVIAL =
            new com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha() {
                @Override public String codificar(String senhaPlana) { return "hash:" + senhaPlana; }
                @Override public boolean verificar(String senhaPlana, String senhaHash) {
                    return senhaHash.equals("hash:" + senhaPlana);
                }
            };

    private UsuarioRepositoryEmMemoria usuarioRepository;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private RegistrarUsuario useCase;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        useCase = new RegistrarUsuario(usuarioRepository, new PerfilAcessoRepositoryEmMemoria(), CODIFICADOR_TRIVIAL, trilha.execucao());
    }

    // -------------------------------------------------------------------------
    // Autocadastro
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("autocadastro: salva usuário com e-mail normalizado, senha codificada e o perfil padrão")
    void autocadastroSalvaUsuarioCorretamente() {
        Usuario criado = useCase.autocadastrar("Ana Silva", " ANA@exemplo.com ", "senha123");

        assertThat(criado.getNome()).isEqualTo("Ana Silva");
        assertThat(criado.getEmail()).isEqualTo("ana@exemplo.com");
        assertThat(criado.getSenhaHash()).isEqualTo("hash:senha123");
        assertThat(criado.getPerfilId()).isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);
        assertThat(criado.getId()).isNotNull();
    }

    @Test
    @DisplayName("autocadastro: persiste o usuário no repositório")
    void autocadastroPersistNoRepositorio() {
        useCase.autocadastrar("João Lima", "joao@exemplo.com", "s3cr3t");

        assertThat(usuarioRepository.todos()).hasSize(1);
        assertThat(usuarioRepository.porEmail("joao@exemplo.com")).isPresent();
    }

    @Test
    @DisplayName("autocadastro: registra evento de auditoria USUARIO_REGISTRADO")
    void autocadastroRegistraEventoDeAuditoria() {
        useCase.autocadastrar("Maria Souza", "maria@exemplo.com", "pw");

        var eventos = trilha.eventos(AcaoAuditoria.USUARIO_REGISTRADO);
        assertThat(eventos).hasSize(1);
        assertThat(eventos.getFirst().getReferencia()).contains("maria@exemplo.com");
    }

    // -------------------------------------------------------------------------
    // Regra: e-mail duplicado
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("rejeita cadastro com e-mail já utilizado (exato)")
    void rejeitalEmailDuplicadoExato() {
        useCase.autocadastrar("Primeiro", "dup@exemplo.com", "pw1");

        assertThatThrownBy(
                () -> useCase.autocadastrar("Segundo", "dup@exemplo.com", "pw2"))
                .isInstanceOf(EmailJaUtilizadoException.class);
    }

    @Test
    @DisplayName("rejeita cadastro com e-mail já utilizado (diferença de caixa)")
    void rejeitaEmailDuplicadoCaseDiferente() {
        useCase.autocadastrar("Primeiro", "dup@exemplo.com", "pw1");

        assertThatThrownBy(
                () -> useCase.autocadastrar("Segundo", "DUP@EXEMPLO.COM", "pw2"))
                .isInstanceOf(EmailJaUtilizadoException.class);
    }

    // -------------------------------------------------------------------------
    // Invariantes de domínio
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("rejeita nome em branco")
    void rejeitaNomeEmBranco() {
        assertThatThrownBy(
                () -> useCase.autocadastrar("  ", "x@exemplo.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nome");
    }

}