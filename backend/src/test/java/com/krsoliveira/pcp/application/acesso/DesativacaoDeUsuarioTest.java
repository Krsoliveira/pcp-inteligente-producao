package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.auth.UsuarioRepositoryEmMemoria;
import com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.EventoAuditoria;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.krsoliveira.pcp.application.acesso.PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR;
import static com.krsoliveira.pcp.application.comum.TrilhaDeAuditoriaEmMemoria.USUARIO_TESTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Desativar e reativar um usuário sem excluí-lo: alcance do gestor, o próprio usuário e o último Administrador ativo. */
class DesativacaoDeUsuarioTest {

    private PerfilAcessoRepositoryEmMemoria perfis;
    private UsuarioRepositoryEmMemoria usuarios;
    private TrilhaDeAuditoriaEmMemoria trilha;
    private Set<Permissao> doAtor;
    private DesativacaoDeUsuario desativacao;
    private AtribuirPerfilAoUsuario atribuir;

    @BeforeEach
    void setUp() {
        perfis = new PerfilAcessoRepositoryEmMemoria();
        usuarios = new UsuarioRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        doAtor = EnumSet.allOf(Permissao.class);
        desativacao = new DesativacaoDeUsuario(usuarios, perfis, () -> doAtor, trilha.execucao());
        atribuir = new AtribuirPerfilAoUsuario(usuarios, perfis, () -> doAtor, trilha.execucao());
    }

    private PerfilAcesso umPerfil(String nome, Permissao... permissoes) {
        return perfis.salvar(PerfilAcesso.criar(nome, "", List.of(permissoes), "sistema"));
    }

    private Usuario umUsuario(String email, UUID perfilId) {
        return usuarios.salvar(Usuario.criar("Fulano", email, "hash", perfilId));
    }

    private Usuario atual(Usuario usuario) {
        return usuarios.porId(usuario.getId()).orElseThrow();
    }

    @Test
    @DisplayName("desativa sem excluir e registra USUARIO_DESATIVADO com o e-mail e o perfil")
    void desativaERegistra() {
        PerfilAcesso operacao = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
        Usuario ana = umUsuario("ana@pcp.com", operacao.getId());

        desativacao.desativar(ana.getId());

        assertThat(atual(ana).isAtivo()).isFalse();
        assertThat(usuarios.todos()).hasSize(1);
        EventoAuditoria evento = trilha.eventos(AcaoAuditoria.USUARIO_DESATIVADO).getFirst();
        assertThat(evento.getReferencia()).isEqualTo("ana@pcp.com");
        assertThat(evento.getDetalhes()).containsEntry("perfil", "Operação");
    }

    @Test
    @DisplayName("reativa com o mesmo perfil e registra USUARIO_REATIVADO")
    void reativa() {
        PerfilAcesso operacao = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
        Usuario ana = umUsuario("ana@pcp.com", operacao.getId());
        desativacao.desativar(ana.getId());

        desativacao.reativar(ana.getId());

        assertThat(atual(ana).isAtivo()).isTrue();
        assertThat(atual(ana).getPerfilId()).isEqualTo(operacao.getId());
        assertThat(trilha.eventos(AcaoAuditoria.USUARIO_REATIVADO)).hasSize(1);
    }

    @Test
    @DisplayName("não desativa nem reativa o próprio usuário (403)")
    void naoMexeEmSiMesmo() {
        Usuario eu = umUsuario(USUARIO_TESTE, ID_ADMINISTRADOR);
        umUsuario("outro-admin@pcp.com", ID_ADMINISTRADOR);

        assertThatThrownBy(() -> desativacao.desativar(eu.getId())).isInstanceOf(AcessoNegadoException.class);
        assertThat(atual(eu).isAtivo()).isTrue();
    }

    @Test
    @DisplayName("só alcança quem tem perfil ao alcance do gestor (403), e nada muda")
    void foraDoAlcance() {
        PerfilAcesso chefia = umPerfil("Chefia", Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR);
        Usuario chefe = umUsuario("chefe@pcp.com", chefia.getId());
        doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.USUARIO_GERENCIAR);

        assertThatThrownBy(() -> desativacao.desativar(chefe.getId())).isInstanceOf(AcessoNegadoException.class);
        assertThat(atual(chefe).isAtivo()).isTrue();
        assertThat(trilha.eventos()).isEmpty();
    }

    @Test
    @DisplayName("não desativa o último Administrador ativo (422)")
    void ultimoAdministradorAtivo() {
        Usuario unico = umUsuario("admin@pcp.com", ID_ADMINISTRADOR);

        assertThatThrownBy(() -> desativacao.desativar(unico.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("último Administrador ativo");
        assertThat(atual(unico).isAtivo()).isTrue();
    }

    @Test
    @DisplayName("desativa um Administrador quando há outro ativo; um desativado não conta como o outro")
    void administradorComOutroAtivo() {
        Usuario primeiro = umUsuario("admin1@pcp.com", ID_ADMINISTRADOR);
        Usuario segundo = umUsuario("admin2@pcp.com", ID_ADMINISTRADOR);

        desativacao.desativar(primeiro.getId());

        assertThatThrownBy(() -> desativacao.desativar(segundo.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("trocar o perfil do último Administrador ativo é recusado, mesmo havendo um Administrador desativado")
    void trocaDePerfilContaSoAtivos() {
        PerfilAcesso operacao = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
        Usuario desativado = umUsuario("admin1@pcp.com", ID_ADMINISTRADOR);
        Usuario ativo = umUsuario("admin2@pcp.com", ID_ADMINISTRADOR);
        desativacao.desativar(desativado.getId());

        assertThatThrownBy(() -> atribuir.executar(ativo.getId(), operacao.getId()))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("desativar quem já está desativado e reativar quem está ativo são recusados (422)")
    void estadoRepetido() {
        PerfilAcesso operacao = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
        Usuario ana = umUsuario("ana@pcp.com", operacao.getId());

        assertThatThrownBy(() -> desativacao.reativar(ana.getId())).isInstanceOf(RegraDeNegocioException.class);
        desativacao.desativar(ana.getId());
        assertThatThrownBy(() -> desativacao.desativar(ana.getId())).isInstanceOf(RegraDeNegocioException.class);
        assertThat(trilha.eventos(AcaoAuditoria.USUARIO_DESATIVADO)).hasSize(1);
    }

    @Test
    @DisplayName("usuário inexistente: 404")
    void inexistente() {
        assertThatThrownBy(() -> desativacao.desativar(UUID.randomUUID())).isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
