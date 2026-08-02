package com.krsoliveira.pcp.domain.usuario;

/**
 * Perfis de acesso dos usuários da plataforma PCP.
 */
public enum Perfil {
    /** Cria e acompanha ordens de produção. */
    PLANEJADOR,
    /** Acesso completo: relatórios, configurações e tudo que o PLANEJADOR faz. */
    GERENTE
}
