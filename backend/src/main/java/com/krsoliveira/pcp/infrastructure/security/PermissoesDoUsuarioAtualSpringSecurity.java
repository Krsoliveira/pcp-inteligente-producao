package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Permissões do usuário logado, lidas das autoridades do contexto do Spring Security. Autoridades
 * que não são permissões conhecidas são ignoradas. Sem login, o conjunto é vazio.
 */
@Component
public class PermissoesDoUsuarioAtualSpringSecurity implements PermissoesDoUsuarioAtual {

    @Override
    public Set<Permissao> permissoes() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<Permissao> permissoes = EnumSet.noneOf(Permissao.class);
        if (auth == null || !auth.isAuthenticated()) {
            return permissoes;
        }
        for (GrantedAuthority autoridade : auth.getAuthorities()) {
            Arrays.stream(Permissao.values())
                    .filter(p -> p.name().equals(autoridade.getAuthority()))
                    .findFirst()
                    .ifPresent(permissoes::add);
        }
        return permissoes;
    }
}
