package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.auth.EmailJaUtilizadoException;
import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
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

/** Criar usuário por um gestor e trocar o perfil de alguém (ADR-0020). */
class UsuariosDeAcessoTest {

    private static final CodificadorDeSenha CODIFICADOR = new CodificadorDeSenha() {
        @Override public String codificar(String senhaPlana) { return "hash:" + senhaPlana; }
        @Override public boolean verificar(String senhaPlana, String senhaHash) { return senhaHash.equals("hash:" + senhaPlana); }
    };

    private PerfilAcessoRepositoryEmMemoria perfis;
    private UsuarioRepositoryEmMemoria usuarios;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private Set<Permissao> doAtor;
    private CriarUsuarioPorGestor criarUsuario;
    private AtribuirPerfilAoUsuario atribuir;

    @BeforeEach
    void setUp() {
        perfis = new PerfilAcessoRepositoryEmMemoria();
        usuarios = new UsuarioRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        doAtor = EnumSet.allOf(Permissao.class);
        var registrar = new RegistrarUsuario(usuarios, perfis, CODIFICADOR, trilha.execucao());
        criarUsuario = new CriarUsuarioPorGestor(perfis, registrar, () -> doAtor, () -> USUARIO_TESTE);
        atribuir = new AtribuirPerfilAoUsuario(usuarios, perfis, () -> doAtor, trilha.execucao());
    }

    private PerfilAcesso umPerfil(String nome, Permissao... permissoes) {
        return perfis.salvar(PerfilAcesso.criar(nome, "", List.of(permissoes), "sistema"));
    }

    private Usuario umUsuario(String email, UUID perfilId) {
        return usuarios.salvar(Usuario.criar("Fulano", email, "hash", perfilId));
    }

    @Nested
    @DisplayName("Criar usuário")
    class Criar {

        @Test
        @DisplayName("cria o usuário com o perfil escolhido, senha codificada, e registra o gestor como responsável")
        void criaComOPerfilEscolhido() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);

            Usuario criado = criarUsuario.executar("Ana", " ANA@pcp.com ", "provisoria1", perfil.getId());

            assertThat(criado.getPerfilId()).isEqualTo(perfil.getId());
            assertThat(criado.getEmail()).isEqualTo("ana@pcp.com");
            assertThat(criado.getSenhaHash()).isEqualTo("hash:provisoria1");
            var evento = trilha.eventos(AcaoAuditoria.USUARIO_REGISTRADO).getFirst();
            assertThat(evento.getUsuario()).isEqualTo(USUARIO_TESTE);
            assertThat(evento.getDetalhes()).containsEntry("perfil", "Operação");
        }

        @Test
        @DisplayName("recusa um perfil com permissões acima do alcance do gestor e não cria o usuário")
        void recusaAcimaDoAlcance() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.LISTA_ATIVAR);
            doAtor = EnumSet.of(Permissao.USUARIO_GERENCIAR, Permissao.ORDEM_VISUALIZAR);

            assertThatThrownBy(() -> criarUsuario.executar("Ana", "ana@pcp.com", "provisoria1", poderoso.getId()))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(usuarios.todos()).isEmpty();
        }

        @Test
        @DisplayName("e-mail repetido e perfil inexistente são recusados")
        void emailRepetidoEPerfilInexistente() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            criarUsuario.executar("Ana", "ana@pcp.com", "provisoria1", perfil.getId());

            assertThatThrownBy(() -> criarUsuario.executar("Outra Ana", "ANA@pcp.com", "provisoria2", perfil.getId()))
                    .isInstanceOf(EmailJaUtilizadoException.class);
            assertThatThrownBy(() -> criarUsuario.executar("Beto", "beto@pcp.com", "provisoria3", UUID.randomUUID()))
                    .isInstanceOf(PerfilNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Trocar o perfil de um usuário")
    class Trocar {

        @Test
        @DisplayName("troca o perfil e registra PERFIL_ATRIBUIDO com o perfil de antes e o de depois")
        void trocaERegistra() {
            PerfilAcesso novo = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            Usuario ana = umUsuario("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            Usuario salvo = atribuir.executar(ana.getId(), novo.getId());

            assertThat(salvo.getPerfilId()).isEqualTo(novo.getId());
            assertThat(usuarios.porId(ana.getId()).orElseThrow().getPerfilId()).isEqualTo(novo.getId());
            var evento = trilha.eventos(AcaoAuditoria.PERFIL_ATRIBUIDO).getFirst();
            assertThat(evento.getReferencia()).isEqualTo("ana@pcp.com");
            assertThat(evento.getDetalhes().get("perfil").toString()).contains("Visualizador", "Operação");
        }

        @Test
        @DisplayName("ninguém altera o próprio perfil")
        void naoAlteraOProprio() {
            PerfilAcesso novo = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            Usuario eu = umUsuario(USUARIO_TESTE, PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            assertThatThrownBy(() -> atribuir.executar(eu.getId(), novo.getId()))
                    .isInstanceOf(AcessoNegadoException.class).hasMessageContaining("próprio perfil");

            assertThat(usuarios.porId(eu.getId()).orElseThrow().getPerfilId())
                    .isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);
        }

        @Test
        @DisplayName("recusa atribuir um perfil acima do alcance de quem atribui")
        void recusaPerfilNovoAcimaDoAlcance() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.LISTA_ATIVAR);
            Usuario ana = umUsuario("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);
            doAtor = EnumSet.of(Permissao.USUARIO_GERENCIAR, Permissao.DASHBOARD_VISUALIZAR, Permissao.LISTA_VISUALIZAR_ATIVAS);

            assertThatThrownBy(() -> atribuir.executar(ana.getId(), poderoso.getId()))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("recusa mexer em quem tem hoje um perfil acima do alcance de quem atribui (não se rebaixa quem está acima)")
        void recusaRebaixarQuemEstaAcima() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.LISTA_ATIVAR);
            Usuario chefe = umUsuario("chefe@pcp.com", poderoso.getId());
            doAtor = EnumSet.of(Permissao.USUARIO_GERENCIAR, Permissao.DASHBOARD_VISUALIZAR, Permissao.LISTA_VISUALIZAR_ATIVAS);

            assertThatThrownBy(() -> atribuir.executar(chefe.getId(), PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(usuarios.porId(chefe.getId()).orElseThrow().getPerfilId()).isEqualTo(poderoso.getId());
        }

        @Test
        @DisplayName("o último Administrador não pode sair do perfil")
        void ultimoAdministrador() {
            Usuario unico = umUsuario("admin@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);

            assertThatThrownBy(() -> atribuir.executar(unico.getId(), PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("último");

            assertThat(usuarios.porId(unico.getId()).orElseThrow().getPerfilId())
                    .isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);
        }

        @Test
        @DisplayName("havendo outro Administrador, um deles pode ser movido")
        void haOutroAdministrador() {
            Usuario primeiro = umUsuario("admin1@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);
            umUsuario("admin2@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR);

            Usuario salvo = atribuir.executar(primeiro.getId(), PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            assertThat(salvo.getPerfilId()).isEqualTo(PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);
        }

        @Test
        @DisplayName("atribuir o mesmo perfil não faz nada e não registra evento")
        void mesmoPerfil() {
            Usuario ana = umUsuario("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            atribuir.executar(ana.getId(), PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("usuário ou perfil inexistentes devolvem não encontrado")
        void inexistentes() {
            Usuario ana = umUsuario("ana@pcp.com", PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR);

            assertThatThrownBy(() -> atribuir.executar(UUID.randomUUID(), PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR))
                    .isInstanceOf(UsuarioNaoEncontradoException.class);
            assertThatThrownBy(() -> atribuir.executar(ana.getId(), UUID.randomUUID()))
                    .isInstanceOf(PerfilNaoEncontradoException.class);
        }
    }
}
