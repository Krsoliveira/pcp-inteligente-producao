package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o Spring Security e o domínio de usuários.
 * Carrega um {@link UserDetails} pelo e-mail para que o framework possa verificar credenciais e popular
 * o {@code SecurityContext}. As autoridades são as permissões do perfil do usuário (ADR-0020), lidas do
 * banco a cada requisição: mudar um perfil, ou o perfil de alguém, vale na hora.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, PerfilAcessoRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.porEmail(email)
                .map(u -> {
                    var autoridades = perfilRepository.buscarPorId(u.getPerfilId())
                            .map(PerfilAcesso::permissoesEfetivas)
                            .orElseGet(java.util.Set::of).stream()
                            .map(Permissao::name)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
                    return (UserDetails) User.builder()
                            .username(u.getEmail())
                            .password(u.getSenhaHash())
                            .authorities(autoridades)
                            .build();
                })
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuário não encontrado: " + email));
    }
}
