package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.application.acesso.PerfilAcessoRepositoryEmMemoria;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdministradorInicialTest {

    private final Map<String, Usuario> usuarios = new HashMap<>();

    private final UsuarioRepository repositorio = new UsuarioRepository() {
        @Override
        public Usuario salvar(Usuario usuario) {
            usuarios.put(usuario.getEmail(), usuario);
            return usuario;
        }

        @Override
        public Optional<Usuario> porEmail(String email) {
            return Optional.ofNullable(usuarios.get(email));
        }

        @Override
        public Optional<Usuario> porId(UUID id) {
            return usuarios.values().stream().filter(u -> u.getId().equals(id)).findFirst();
        }

        @Override
        public List<Usuario> listarTodos() {
            return new ArrayList<>(usuarios.values());
        }

        @Override
        public long contarPorPerfil(UUID perfilId) {
            return usuarios.values().stream().filter(u -> u.getPerfilId().equals(perfilId)).count();
        }
    };

    private final PerfilAcessoRepositoryEmMemoria perfis = new PerfilAcessoRepositoryEmMemoria();

    private final CodificadorDeSenha codificador = new CodificadorDeSenha() {
        @Override
        public String codificar(String senhaPlana) {
            return "hash:" + senhaPlana;
        }

        @Override
        public boolean verificar(String senhaPlana, String senhaHash) {
            return senhaHash.equals("hash:" + senhaPlana);
        }
    };

    private final TrilhaDeAuditoriaEmMemoria trilha = new TrilhaDeAuditoriaEmMemoria();
    private final RegistrarUsuario registrarUsuario =
            new RegistrarUsuario(repositorio, perfis, codificador, trilha.execucao());

    private AdministradorInicial com(String nome, String email, String senha) {
        return new AdministradorInicial(repositorio, registrarUsuario, perfis, nome, email, senha);
    }

    @Test
    @DisplayName("sem ADMIN_EMAIL não cria ninguém")
    void semConfiguracao() {
        com("", "", "").run(null);

        assertThat(usuarios).isEmpty();
    }

    @Test
    @DisplayName("cria o administrador com o perfil Administrador, e-mail normalizado e senha codificada")
    void criaAdministrador() {
        com("Admin Teste", " Admin@PCP.dev ", "SenhaForte123").run(null);

        Usuario admin = usuarios.get("admin@pcp.dev");
        assertThat(admin).isNotNull();
        assertThat(admin.getPerfilId()).isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);
        assertThat(admin.getSenhaHash()).isEqualTo("hash:SenhaForte123");
        assertThat(trilha.eventos(AcaoAuditoria.USUARIO_REGISTRADO)).singleElement()
                .satisfies(evento -> assertThat(evento.getUsuario())
                        .isEqualTo(AdministradorInicial.RESPONSAVEL));
    }

    @Test
    @DisplayName("é idempotente — segunda inicialização não duplica nem altera")
    void idempotente() {
        AdministradorInicial bootstrap = com("Admin Teste", "admin@pcp.dev", "SenhaForte123");
        bootstrap.run(null);
        Usuario primeiro = usuarios.get("admin@pcp.dev");

        bootstrap.run(null);

        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get("admin@pcp.dev")).isSameAs(primeiro);
    }

    @Test
    @DisplayName("não promove conta já existente com o mesmo e-mail")
    void naoPromoveContaExistente() {
        registrarUsuario.autocadastrar("Outra Pessoa", "admin@pcp.dev", "senhaDela123");

        com("Admin Teste", "admin@pcp.dev", "SenhaForte123").run(null);

        Usuario conta = usuarios.get("admin@pcp.dev");
        assertThat(conta.getPerfilId()).isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);
        assertThat(conta.getSenhaHash()).isEqualTo("hash:senhaDela123");
    }

    @Test
    @DisplayName("configuração incompleta impede a inicialização")
    void configuracaoIncompleta() {
        assertThatThrownBy(() -> com("", "admin@pcp.dev", "SenhaForte123").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_NOME");
        assertThatThrownBy(() -> com("Admin", "admin@pcp.dev", "curta").run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_SENHA");
        assertThat(usuarios).isEmpty();
    }
}
