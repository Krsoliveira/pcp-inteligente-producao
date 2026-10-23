package com.krsoliveira.pcp.infrastructure.security;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Ponte entre o Spring Security e o domínio de usuários.
 * Carrega um {@link UserDetails} pelo e-mail para que o framework possa verificar credenciais e popular
 * o {@code SecurityContext}. As autoridades são as permissões do perfil do usuário (ADR-0020), lidas do
 * banco a cada requisição: mudar um perfil, ou o perfil de alguém, vale na hora.
 *
 * Enquanto o usuário precisa trocar a senha (ADR-0021) ele não recebe permissão nenhuma, só a autoridade
 * interna {@value #SENHA_PROVISORIA}: toda anotação de permissão falha (403) e restam apenas os endpoints
 * sem permissão, que são "quem sou eu" e a troca de senha.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    /** Autoridade interna de quem precisa trocar a senha; não é uma permissão do catálogo. */
    public static final String SENHA_PROVISORIA = "SENHA_PROVISORIA";

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository, PerfilAcessoRepository perfilRepository) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.porEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getSenhaHash())
                .authorities(autoridadesDe(usuario))
                .build();
    }

    private List<GrantedAuthority> autoridadesDe(Usuario usuario) {
        if (usuario.isDeveTrocarSenha()) {
            return List.of(new SimpleGrantedAuthority(SENHA_PROVISORIA));
        }
        Set<Permissao> permissoes = perfilRepository.buscarPorId(usuario.getPerfilId())
                .map(PerfilAcesso::permissoesEfetivas)
                .orElseGet(Set::of);
        return permissoes.stream().map(Permissao::name).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList();
    }
}
