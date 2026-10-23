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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Criar, alterar e excluir perfis de acesso (ADR-0020): a regra "só se concede o que se tem", os
 * perfis de sistema, o nome único e a trilha de auditoria.
 */
class PerfisDeAcessoTest {

    private PerfilAcessoRepositoryEmMemoria perfis;
    private UsuarioRepositoryEmMemoria usuarios;
    private TrilhaDeAuditoriaEmMemoria trilha;
    /** Permissões de quem está agindo; cada teste ajusta. */
    private Set<Permissao> doAtor;

    private CriarPerfilAcesso criar;
    private AtualizarPerfilAcesso atualizar;
    private ExcluirPerfilAcesso excluir;

    @BeforeEach
    void setUp() {
        perfis = new PerfilAcessoRepositoryEmMemoria();
        usuarios = new UsuarioRepositoryEmMemoria();
        trilha = new TrilhaDeAuditoriaEmMemoria();
        doAtor = EnumSet.allOf(Permissao.class);
        criar = new CriarPerfilAcesso(perfis, () -> doAtor, trilha.execucao());
        atualizar = new AtualizarPerfilAcesso(perfis, () -> doAtor, trilha.execucao());
        excluir = new ExcluirPerfilAcesso(perfis, usuarios, () -> doAtor, trilha.execucao());
    }

    private PerfilAcesso umPerfil(String nome, Permissao... permissoes) {
        return perfis.salvar(PerfilAcesso.criar(nome, "", List.of(permissoes), "sistema"));
    }

    private EventoAuditoria unicoEvento(AcaoAuditoria acao) {
        assertThat(trilha.eventos(acao)).hasSize(1);
        return trilha.eventos(acao).getFirst();
    }

    @Nested
    @DisplayName("Criar perfil")
    class Criar {

        @Test
        @DisplayName("cria dentro do alcance de quem cria e registra CRIADO com as permissões")
        void criaDentroDoAlcance() {
            doAtor = EnumSet.of(Permissao.LISTA_COMPARAR, Permissao.LISTA_VISUALIZAR_TODAS, Permissao.PERFIL_GERENCIAR);

            PerfilAcesso criado = criar.executar("Qualidade", "Confere listas",
                    List.of(Permissao.LISTA_COMPARAR, Permissao.LISTA_VISUALIZAR_TODAS));

            assertThat(perfis.buscarPorId(criado.getId())).isPresent();
            EventoAuditoria evento = unicoEvento(AcaoAuditoria.CRIADO);
            assertThat(evento.getReferencia()).isEqualTo("Qualidade");
            assertThat(evento.getDetalhes().get("permissoes").toString())
                    .contains("LISTA_COMPARAR").contains("LISTA_VISUALIZAR_TODAS");
        }

        @Test
        @DisplayName("recusa permissões que passam do alcance de quem cria e não grava nada")
        void recusaAcimaDoAlcance() {
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> criar.executar("Operação", "", List.of(Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR)))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(perfis.buscarPorNome("Operação")).isEmpty();
            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("recusa nome repetido, sem diferenciar maiúsculas")
        void recusaNomeRepetido() {
            umPerfil("Qualidade", Permissao.ORDEM_VISUALIZAR);

            assertThatThrownBy(() -> criar.executar("QUALIDADE", "", List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(PerfilNomeJaUtilizadoException.class);
        }

        @Test
        @DisplayName("valida os dados do perfil (nome vazio, sem permissões)")
        void validaDados() {
            assertThatThrownBy(() -> criar.executar(" ", "", List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(RegraDeNegocioException.class);
            assertThatThrownBy(() -> criar.executar("Vazio", "", List.of()))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
    }

    @Nested
    @DisplayName("Alterar perfil")
    class Alterar {

        @Test
        @DisplayName("altera nome e permissões e registra ALTERADO com o que entrou e saiu")
        void alteraERegistra() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR);

            atualizar.executar(perfil.getId(), "Operação de chão", "", List.of(Permissao.ORDEM_VISUALIZAR, Permissao.ESTOQUE_VISUALIZAR));

            PerfilAcesso salvo = perfis.buscarPorId(perfil.getId()).orElseThrow();
            assertThat(salvo.getNome()).isEqualTo("Operação de chão");
            assertThat(salvo.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.ORDEM_VISUALIZAR, Permissao.ESTOQUE_VISUALIZAR);
            EventoAuditoria evento = unicoEvento(AcaoAuditoria.ALTERADO);
            assertThat(evento.getDetalhes().get("permissoesAdicionadas")).isEqualTo("ESTOQUE_VISUALIZAR");
            assertThat(evento.getDetalhes().get("permissoesRemovidas")).isEqualTo("ORDEM_CRIAR");
            assertThat(evento.getDetalhes().get("nome").toString()).contains("Operação", "Operação de chão");
        }

        @Test
        @DisplayName("só mudar o nome não registra permissões adicionadas nem removidas")
        void soMudaONome() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);

            atualizar.executar(perfil.getId(), "Chão", "", List.of(Permissao.ORDEM_VISUALIZAR));

            EventoAuditoria evento = unicoEvento(AcaoAuditoria.ALTERADO);
            assertThat(evento.getDetalhes()).doesNotContainKeys("permissoesAdicionadas", "permissoesRemovidas");
        }

        @Test
        @DisplayName("o Administrador não pode ser alterado")
        void administradorNaoAltera() {
            assertThatThrownBy(() -> atualizar.executar(PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR, "Outro", "",
                    List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Administrador");
        }

        @Test
        @DisplayName("recusa alterar um perfil que tem permissões acima do alcance de quem altera")
        void recusaPerfilAcimaDoAlcance() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR);
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> atualizar.executar(poderoso.getId(), "Chefia 2", "", List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(perfis.buscarPorId(poderoso.getId()).orElseThrow().getNome()).isEqualTo("Chefia");
        }

        @Test
        @DisplayName("recusa elevar um perfil além do alcance de quem altera")
        void recusaElevarAlemDoAlcance() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> atualizar.executar(perfil.getId(), "Operação", "",
                    List.of(Permissao.ORDEM_VISUALIZAR, Permissao.USUARIO_GERENCIAR)))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(perfis.buscarPorId(perfil.getId()).orElseThrow().permissoesEfetivas())
                    .containsExactly(Permissao.ORDEM_VISUALIZAR);
            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("recusa o nome de outro perfil, mas aceita manter o próprio nome")
        void nomeRepetido() {
            PerfilAcesso a = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            umPerfil("Qualidade", Permissao.ORDEM_VISUALIZAR);

            assertThatThrownBy(() -> atualizar.executar(a.getId(), "qualidade", "", List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(PerfilNomeJaUtilizadoException.class);
            atualizar.executar(a.getId(), "OPERAÇÃO", "nova descrição", List.of(Permissao.ORDEM_VISUALIZAR));

            assertThat(perfis.buscarPorId(a.getId()).orElseThrow().getNome()).isEqualTo("OPERAÇÃO");
        }

        @Test
        @DisplayName("perfil inexistente devolve não encontrado")
        void inexistente() {
            assertThatThrownBy(() -> atualizar.executar(UUID.randomUUID(), "X", "", List.of(Permissao.ORDEM_VISUALIZAR)))
                    .isInstanceOf(PerfilNaoEncontradoException.class);
        }
    }

    @Nested
    @DisplayName("Excluir perfil")
    class Excluir {

        @Test
        @DisplayName("exclui um perfil sem usuários e registra EXCLUIDO")
        void excluiSemUsuarios() {
            PerfilAcesso perfil = umPerfil("Temporário", Permissao.ORDEM_VISUALIZAR);

            excluir.executar(perfil.getId());

            assertThat(perfis.buscarPorId(perfil.getId())).isEmpty();
            assertThat(unicoEvento(AcaoAuditoria.EXCLUIDO).getReferencia()).isEqualTo("Temporário");
        }

        @Test
        @DisplayName("perfis de sistema (Administrador e o padrão do autocadastro) não podem ser excluídos")
        void perfisDeSistema() {
            assertThatThrownBy(() -> excluir.executar(PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("sistema");
            assertThatThrownBy(() -> excluir.executar(PerfilAcessoRepositoryEmMemoria.ID_VISUALIZADOR))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("sistema");
        }

        @Test
        @DisplayName("recusa excluir um perfil que algum usuário usa, dizendo quantos")
        void recusaComUsuarios() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            usuarios.salvar(Usuario.criar("Ana", "ana@pcp", "hash", perfil.getId()));
            usuarios.salvar(Usuario.criar("Beto", "beto@pcp", "hash", perfil.getId()));

            assertThatThrownBy(() -> excluir.executar(perfil.getId()))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("2 usuários");

            assertThat(perfis.buscarPorId(perfil.getId())).isPresent();
            assertThat(trilha.eventos()).isEmpty();
        }

        @Test
        @DisplayName("recusa excluir um perfil acima do alcance de quem exclui")
        void recusaAcimaDoAlcance() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.LISTA_ATIVAR);
            doAtor = EnumSet.of(Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> excluir.executar(poderoso.getId())).isInstanceOf(AcessoNegadoException.class);

            assertThat(perfis.buscarPorId(poderoso.getId())).isPresent();
        }

        @Test
        @DisplayName("perfil inexistente devolve não encontrado")
        void inexistente() {
            assertThatThrownBy(() -> excluir.executar(UUID.randomUUID())).isInstanceOf(PerfilNaoEncontradoException.class);
        }
    }
}
