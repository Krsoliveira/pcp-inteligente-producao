package com.krsoliveira.pcp.domain.acesso;

import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.auditoria.Assinatura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PerfilAcessoTest {

    private static final String USUARIO = "gerente@pcp";

    private static PerfilAcesso umAdministrador() {
        return PerfilAcesso.reconstituir(UUID.randomUUID(), "Administrador", "", true, false, Set.of(),
                Assinatura.nova("sistema"));
    }

    @Test
    @DisplayName("cria perfil com nome aparado, descrição e permissões; nunca é de sistema")
    void criaPerfil() {
        PerfilAcesso perfil = PerfilAcesso.criar("  Qualidade  ", " Confere as listas ",
                List.of(Permissao.LISTA_COMPARAR, Permissao.LISTA_VISUALIZAR_TODAS), USUARIO);

        assertThat(perfil.getId()).isNotNull();
        assertThat(perfil.getNome()).isEqualTo("Qualidade");
        assertThat(perfil.getDescricao()).isEqualTo("Confere as listas");
        assertThat(perfil.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.LISTA_COMPARAR, Permissao.LISTA_VISUALIZAR_TODAS);
        assertThat(perfil.isAdministrador()).isFalse();
        assertThat(perfil.isPadraoDoAutocadastro()).isFalse();
        assertThat(perfil.podeSerExcluido()).isTrue();
        assertThat(perfil.getAssinatura().criadoPor()).isEqualTo(USUARIO);
    }

    @Test
    @DisplayName("permissões repetidas contam uma vez")
    void permissoesRepetidas() {
        PerfilAcesso perfil = PerfilAcesso.criar("Operação", null,
                List.of(Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_VISUALIZAR), USUARIO);

        assertThat(perfil.permissoesEfetivas()).containsExactly(Permissao.ORDEM_VISUALIZAR);
        assertThat(perfil.getDescricao()).isEmpty();
    }

    @Test
    @DisplayName("rejeita nome vazio, nome longo demais, descrição longa demais e perfil sem permissões")
    void validacoes() {
        assertThatThrownBy(() -> PerfilAcesso.criar("  ", "", List.of(Permissao.ORDEM_VISUALIZAR), USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("nome");
        assertThatThrownBy(() -> PerfilAcesso.criar("x".repeat(61), "", List.of(Permissao.ORDEM_VISUALIZAR), USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("60");
        assertThatThrownBy(() -> PerfilAcesso.criar("Ok", "d".repeat(201), List.of(Permissao.ORDEM_VISUALIZAR), USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("200");
        assertThatThrownBy(() -> PerfilAcesso.criar("Ok", "", List.of(), USUARIO))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("ao menos uma permissão");
        assertThatThrownBy(() -> PerfilAcesso.criar("Ok", "", null, USUARIO))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("redefinir troca nome, descrição e permissões e assina a alteração")
    void redefine() {
        PerfilAcesso perfil = PerfilAcesso.criar("Operação", "", List.of(Permissao.ORDEM_VISUALIZAR), USUARIO);

        perfil.redefinir("Operação de chão", "Chão de fábrica", List.of(Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR), "outro@pcp");

        assertThat(perfil.getNome()).isEqualTo("Operação de chão");
        assertThat(perfil.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR);
        assertThat(perfil.getAssinatura().alteradoPor()).isEqualTo("outro@pcp");
        assertThat(perfil.getAssinatura().criadoPor()).isEqualTo(USUARIO);
    }

    @Test
    @DisplayName("redefinir com dado inválido não altera nada do perfil")
    void redefinirInvalidoNaoAltera() {
        PerfilAcesso perfil = PerfilAcesso.criar("Operação", "d", List.of(Permissao.ORDEM_VISUALIZAR), USUARIO);

        assertThatThrownBy(() -> perfil.redefinir("Novo nome", "d", List.of(), "x"))
                .isInstanceOf(RegraDeNegocioException.class);

        assertThat(perfil.getNome()).isEqualTo("Operação");
        assertThat(perfil.permissoesEfetivas()).containsExactly(Permissao.ORDEM_VISUALIZAR);
    }

    @Test
    @DisplayName("o Administrador tem todas as permissões, inclusive as futuras, e não pode ser alterado nem excluído")
    void administrador() {
        PerfilAcesso admin = umAdministrador();

        assertThat(admin.permissoesEfetivas()).containsExactlyInAnyOrder(Permissao.values());
        assertThat(admin.podeSerExcluido()).isFalse();
        assertThatThrownBy(() -> admin.redefinir("Outro", "", List.of(Permissao.ORDEM_VISUALIZAR), "x"))
                .isInstanceOf(RegraDeNegocioException.class).hasMessageContaining("Administrador");
    }

    @Test
    @DisplayName("o perfil padrão do autocadastro pode ter as permissões editadas, mas não pode ser excluído")
    void perfilPadrao() {
        PerfilAcesso padrao = PerfilAcesso.reconstituir(UUID.randomUUID(), "Visualizador", "", false, true,
                Set.of(Permissao.DASHBOARD_VISUALIZAR), Assinatura.nova("sistema"));

        padrao.redefinir("Visualizador", "", List.of(Permissao.DASHBOARD_VISUALIZAR, Permissao.ORDEM_VISUALIZAR), "g");

        assertThat(padrao.permissoesEfetivas()).hasSize(2);
        assertThat(padrao.podeSerExcluido()).isFalse();
    }

    @Test
    @DisplayName("o conjunto de permissões devolvido não pode ser alterado por fora")
    void conjuntoImutavel() {
        PerfilAcesso perfil = PerfilAcesso.criar("Operação", "", List.of(Permissao.ORDEM_VISUALIZAR), USUARIO);

        assertThatThrownBy(() -> perfil.permissoesEfetivas().add(Permissao.PERFIL_GERENCIAR))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
