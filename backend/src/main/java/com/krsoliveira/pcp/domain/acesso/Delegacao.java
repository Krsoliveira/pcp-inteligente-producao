package com.krsoliveira.pcp.domain.acesso;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Regra "só se concede o que se tem" (ADR-0020): quem cria ou edita um perfil, ou atribui um perfil
 * a alguém, só pode dar permissões que ele mesmo possui. É o que impede um gerente de se promover
 * (ou promover outro) além do próprio alcance.
 */
public final class Delegacao {

    private Delegacao() {}

    public static void exigirQueAbrange(Set<Permissao> doAtor, Collection<Permissao> pedidas) {
        Set<Permissao> acima = pedidas.isEmpty() ? EnumSet.noneOf(Permissao.class) : EnumSet.copyOf(pedidas);
        acima.removeAll(doAtor);
        if (!acima.isEmpty()) {
            throw new AcessoNegadoException("Você não pode conceder permissões que não tem: %s."
                    .formatted(acima.stream().map(Permissao::getDescricao).sorted().collect(Collectors.joining("; "))));
        }
    }
}
