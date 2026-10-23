package com.krsoliveira.pcp.domain.acesso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegacaoTest {

    @Test
    @DisplayName("aceita conceder o que se tem, inclusive tudo igual ou menos")
    void aceitaOQueTem() {
        Set<Permissao> doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR, Permissao.ORDEM_CRIAR, Permissao.LISTA_COMPARAR);

        assertThatCode(() -> Delegacao.exigirQueAbrange(doAtor, List.of(Permissao.ORDEM_VISUALIZAR))).doesNotThrowAnyException();
        assertThatCode(() -> Delegacao.exigirQueAbrange(doAtor, doAtor)).doesNotThrowAnyException();
        assertThatCode(() -> Delegacao.exigirQueAbrange(doAtor, List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("recusa conceder o que não se tem e diz quais permissões passam do alcance")
    void recusaOQueNaoTem() {
        Set<Permissao> doAtor = EnumSet.of(Permissao.ORDEM_VISUALIZAR);

        assertThatThrownBy(() -> Delegacao.exigirQueAbrange(doAtor,
                List.of(Permissao.ORDEM_VISUALIZAR, Permissao.PERFIL_GERENCIAR, Permissao.LISTA_ATIVAR)))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining(Permissao.PERFIL_GERENCIAR.getDescricao())
                .hasMessageContaining(Permissao.LISTA_ATIVAR.getDescricao())
                .hasMessageNotContaining(Permissao.ORDEM_VISUALIZAR.getDescricao());
    }

    @Test
    @DisplayName("quem não tem nada não concede nada")
    void semPermissoes() {
        assertThatThrownBy(() -> Delegacao.exigirQueAbrange(Set.of(), List.of(Permissao.ORDEM_VISUALIZAR)))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
