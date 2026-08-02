package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o Spring Security e o domínio de usuários.
 * Carrega um {@link UserDetails} pelo e-mail para que o framework
 * possa verificar credenciais e popular o {@code SecurityContext}.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.porEmail(email)
                .map(u -> User.builder()
                        .username(u.getEmail())
                        .password(u.getSenhaHash())
                        .roles(u.getPerfil().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado: " + email));
    }
}
