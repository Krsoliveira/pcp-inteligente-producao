package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.auth.UsuarioRepositoryEmMemoria;
import com.krsoliveira.pcp.application.comum.EdicaoConcorrenteException;
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

            atualizar.executar(perfil.getId(), "Operação de chão", "", List.of(Permissao.ORDEM_VISUALIZAR, Permissao.ESTOQUE_VISUALIZAR), null);

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

            atualizar.executar(perfil.getId(), "Chão", "", List.of(Permissao.ORDEM_VISUALIZAR), null);

            EventoAuditoria evento = unicoEvento(AcaoAuditoria.ALTERADO);
            assertThat(evento.getDetalhes()).doesNotContainKeys("permissoesAdicionadas", "permissoesRemovidas");
        }

        @Test
        @DisplayName("o Administrador não pode ser alterado")
        void administradorNaoAltera() {
            assertThatThrownBy(() -> atualizar.executar(PerfilAcessoRepositoryEmMemoria.ID_ADMINISTRADOR, "Outro", "",
                    List.of(Permissao.ORDEM_VISUALIZAR), null))
                    .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Administrador");
        }

        @Test
        @DisplayName("recusa alterar um perfil que tem permissões acima do alcance de quem altera")
        void recusaPerfilAcimaDoAlcance() {
            PerfilAcesso poderoso = umPerfil("Chefia", Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR);
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> atualizar.executar(poderoso.getId(), "Chefia 2", "", List.of(Permissao.ORDEM_VISUALIZAR), null))
                    .isInstanceOf(AcessoNegadoException.class);

            assertThat(perfis.buscarPorId(poderoso.getId()).orElseThrow().getNome()).isEqualTo("Chefia");
        }

        @Test
        @DisplayName("recusa elevar um perfil além do alcance de quem altera")
        void recusaElevarAlemDoAlcance() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> atualizar.executar(perfil.getId(), "Operação", "",
                    List.of(Permissao.ORDEM_VISUALIZAR, Permissao.USUARIO_GERENCIAR), null))
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

            assertThatThrownBy(() -> atualizar.executar(a.getId(), "qualidade", "", List.of(Permissao.ORDEM_VISUALIZAR), null))
                    .isInstanceOf(PerfilNomeJaUtilizadoException.class);
            atualizar.executar(a.getId(), "OPERAÇÃO", "nova descrição", List.of(Permissao.ORDEM_VISUALIZAR), null);

            assertThat(perfis.buscarPorId(a.getId()).orElseThrow().getNome()).isEqualTo("OPERAÇÃO");
        }

        @Test
        @DisplayName("perfil inexistente devolve não encontrado")
        void inexistente() {
            assertThatThrownBy(() -> atualizar.executar(UUID.randomUUID(), "X", "", List.of(Permissao.ORDEM_VISUALIZAR), null))
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

    @Nested
    @DisplayName("Edição simultânea (ADR-0034)")
    class EdicaoSimultanea {

        @Test
        @DisplayName("com a versão atual, altera e passa para a versão seguinte")
        void versaoAtualAltera() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);

            PerfilAcesso salvo = atualizar.executar(perfil.getId(), "Operação", "nova", List.of(Permissao.ORDEM_VISUALIZAR), 0L);

            assertThat(salvo.getVersao()).isEqualTo(1);
            assertThat(salvo.getDescricao()).isEqualTo("nova");
        }

        @Test
        @DisplayName("com uma versão que já não é a atual, recusa com 409 e não altera nem registra nada")
        void versaoAntigaRecusada() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR);
            // Ana e Bruno abriram o perfil na versão 0; Ana tirou ORDEM_CRIAR e salvou primeiro.
            atualizar.executar(perfil.getId(), "Operação", "", List.of(Permissao.ORDEM_VISUALIZAR), 0L);

            assertThatThrownBy(() -> atualizar.executar(perfil.getId(), "Operação", "do Bruno",
                    List.of(Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR), 0L))
                    .isInstanceOf(EdicaoConcorrenteException.class)
                    .hasMessageContaining("\"Operação\" foi alterado por outra pessoa");

            PerfilAcesso atual = perfis.buscarPorId(perfil.getId()).orElseThrow();
            assertThat(atual.permissoesEfetivas()).containsExactly(Permissao.ORDEM_VISUALIZAR);
            assertThat(atual.getDescricao()).isEmpty();
            assertThat(atual.getVersao()).isEqualTo(1);
            assertThat(trilha.eventos(AcaoAuditoria.ALTERADO)).hasSize(1);
        }

        @Test
        @DisplayName("sem versão informada (cliente que não a envia), altera como antes e ainda soma a versão")
        void semVersaoAlteraComoAntes() {
            PerfilAcesso perfil = umPerfil("Operação", Permissao.ORDEM_VISUALIZAR);
            atualizar.executar(perfil.getId(), "Operação", "a", List.of(Permissao.ORDEM_VISUALIZAR), null);

            PerfilAcesso salvo = atualizar.executar(perfil.getId(), "Operação", "b", List.of(Permissao.ORDEM_VISUALIZAR), null);

            assertThat(salvo.getDescricao()).isEqualTo("b");
            assertThat(salvo.getVersao()).isEqualTo(2);
        }

        @Test
        @DisplayName("quem não alcança o perfil recebe 403, não 409, mesmo com versão antiga")
        void autorizacaoAntesDaVersao() {
            PerfilAcesso perfil = umPerfil("Chefia", Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR);
            atualizar.executar(perfil.getId(), "Chefia", "x", List.of(Permissao.ORDEM_VISUALIZAR, Permissao.LISTA_ATIVAR), 0L);
            doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR);

            assertThatThrownBy(() -> atualizar.executar(perfil.getId(), "Chefia", "",
                    List.of(Permissao.ORDEM_VISUALIZAR), 0L))
                    .isInstanceOf(AcessoNegadoException.class);
        }
    }
}
