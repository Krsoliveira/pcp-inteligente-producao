package com.krsoliveira.pcp.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.time.Instant;
import java.util.Collection;

/** O usuário carregado para o Spring Security, com o que é preciso para conferir se um token ainda vale. */
class UsuarioAutenticado extends User {

    private final transient Instant senhaAlteradaEm;

    UsuarioAutenticado(String email, String senhaHash, boolean ativo, Collection<? extends GrantedAuthority> autoridades,
                       Instant senhaAlteradaEm) {
        super(email, senhaHash, ativo, true, true, true, autoridades);
        this.senhaAlteradaEm = senhaAlteradaEm;
    }

    /** Nulo: a senha não mudou desde que esta informação passou a ser guardada. */
    Instant getSenhaAlteradaEm() {
        return senhaAlteradaEm;
    }
}
