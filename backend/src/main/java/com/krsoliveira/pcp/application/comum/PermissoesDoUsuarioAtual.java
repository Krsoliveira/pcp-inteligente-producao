package com.krsoliveira.pcp.application.comum;

import com.krsoliveira.pcp.domain.acesso.Permissao;

import java.util.Set;

/**
 * Porta: as permissões de quem está executando a operação atual. A implementação lê as autoridades
 * do contexto de segurança; nos testes, um conjunto fixo. Usada pelas regras de delegação
 * ("só se concede o que se tem", ADR-0020).
 */
@FunctionalInterface
public interface PermissoesDoUsuarioAtual {

    Set<Permissao> permissoes();
}
