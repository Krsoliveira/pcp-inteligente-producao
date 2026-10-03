package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.application.comum.UsuarioAtual;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Usuário responsável pela operação, lido do contexto do Spring Security (o e-mail do
 * token JWT). Sem login — o que só ocorre em processos internos — retorna
 * {@value #SISTEMA}, para que nenhum registro fique sem autor.
 */
@Component
public class UsuarioAtualSpringSecurity implements UsuarioAtual {

    static final String SISTEMA = "sistema";

    @Override
    public String identificador() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return SISTEMA;
        }
        return auth.getName();
    }
}
