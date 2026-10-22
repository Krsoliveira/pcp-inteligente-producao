package com.krsoliveira.pcp.application.comum;

/**
 * Porta: identifica quem está executando a operação atual (o usuário logado).
 * A implementação lê o contexto de segurança; nos testes, um valor fixo.
 */
@FunctionalInterface
public interface UsuarioAtual {

    String identificador();
}
