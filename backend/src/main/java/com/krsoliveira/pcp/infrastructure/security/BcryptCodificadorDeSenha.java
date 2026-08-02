package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementação do port {@link CodificadorDeSenha} usando BCrypt via Spring Security.
 * É o único lugar no projeto que "sabe" que estamos usando BCrypt.
 */
@Component
public class BcryptCodificadorDeSenha implements CodificadorDeSenha {

    private final PasswordEncoder passwordEncoder;

    public BcryptCodificadorDeSenha(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String codificar(String senhaPlana) {
        return passwordEncoder.encode(senhaPlana);
    }

    @Override
    public boolean verificar(String senhaPlana, String senhaHash) {
        return passwordEncoder.matches(senhaPlana, senhaHash);
    }
}
