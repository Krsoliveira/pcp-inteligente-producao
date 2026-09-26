package com.krsoliveira.pcp.infrastructure.config;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.Perfil;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
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
    };

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

    private final RegistrarUsuario registrarUsuario = new RegistrarUsuario(repositorio, codificador);

    private AdministradorInicial com(String nome, String email, String senha) {
        return new AdministradorInicial(repositorio, registrarUsuario, nome, email, senha);
    }

    @Test
    @DisplayName("sem ADMIN_EMAIL não cria ninguém")
    void semConfiguracao() {
        com("", "", "").run(null);

        assertThat(usuarios).isEmpty();
    }

    @Test
    @DisplayName("cria o administrador como GERENTE, com e-mail normalizado e senha codificada")
    void criaAdministrador() {
        com("Admin Teste", " Admin@PCP.dev ", "SenhaForte123").run(null);

        Usuario admin = usuarios.get("admin@pcp.dev");
        assertThat(admin).isNotNull();
        assertThat(admin.getPerfil()).isEqualTo(Perfil.GERENTE);
        assertThat(admin.getSenhaHash()).isEqualTo("hash:SenhaForte123");
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
        registrarUsuario.executar("Outra Pessoa", "admin@pcp.dev", "senhaDela123", Perfil.PLANEJADOR);

        com("Admin Teste", "admin@pcp.dev", "SenhaForte123").run(null);

        Usuario conta = usuarios.get("admin@pcp.dev");
        assertThat(conta.getPerfil()).isEqualTo(Perfil.PLANEJADOR);
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
