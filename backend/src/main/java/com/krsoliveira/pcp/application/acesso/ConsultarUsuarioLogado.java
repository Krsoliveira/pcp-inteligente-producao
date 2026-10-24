package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.UsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.Set;

/**
 * Caso de uso: quem sou eu e o que posso fazer. A tela usa o resultado para mostrar ou esconder
 * telas e botões; a barreira de verdade continua no servidor.
 */
public class ConsultarUsuarioLogado {

    /** Com a troca de senha pendente (ADR-0021) o usuário ainda não tem permissão nenhuma. */
    public record Resultado(Usuario usuario, PerfilAcesso perfil, Set<Permissao> permissoes, boolean deveTrocarSenha) {}

    private final UsuarioAtual usuarioAtual;
    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;

    public ConsultarUsuarioLogado(UsuarioAtual usuarioAtual, UsuarioRepository usuarioRepository,
                                  PerfilAcessoRepository perfilRepository) {
        this.usuarioAtual = usuarioAtual;
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
    }

    public Resultado executar() {
        String email = usuarioAtual.identificador();
        Usuario usuario = usuarioRepository.porEmail(email)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        PerfilAcesso perfil = perfilRepository.buscarPorId(usuario.getPerfilId())
                .orElseThrow(() -> new PerfilNaoEncontradoException(usuario.getPerfilId()));
        return new Resultado(usuario, perfil,
                usuario.isDeveTrocarSenha() ? Set.of() : perfil.permissoesEfetivas(), usuario.isDeveTrocarSenha());
    }
}
