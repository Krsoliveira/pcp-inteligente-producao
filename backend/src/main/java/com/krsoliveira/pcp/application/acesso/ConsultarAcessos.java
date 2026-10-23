package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Caso de uso: consultar o catálogo de permissões, os perfis e os usuários com o perfil de cada um. */
public class ConsultarAcessos {

    /** Um perfil com a quantidade de usuários que o usam. */
    public record PerfilComUsuarios(PerfilAcesso perfil, long quantidadeDeUsuarios) {}

    /** Um usuário com o perfil que tem (o nome do perfil vem junto para a tela). */
    public record UsuarioComPerfil(Usuario usuario, PerfilAcesso perfil) {}

    private final PerfilAcessoRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;

    public ConsultarAcessos(PerfilAcessoRepository perfilRepository, UsuarioRepository usuarioRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Todas as permissões que existem, agrupáveis por área. */
    public List<Permissao> permissoes() {
        return Arrays.asList(Permissao.values());
    }

    public List<PerfilComUsuarios> listarPerfis() {
        return perfilRepository.listarTodos().stream()
                .map(p -> new PerfilComUsuarios(p, usuarioRepository.contarPorPerfil(p.getId())))
                .toList();
    }

    public PerfilComUsuarios buscarPerfil(UUID id) {
        PerfilAcesso perfil = perfilRepository.buscarPorId(id).orElseThrow(() -> new PerfilNaoEncontradoException(id));
        return new PerfilComUsuarios(perfil, usuarioRepository.contarPorPerfil(id));
    }

    public List<UsuarioComPerfil> listarUsuarios() {
        Map<UUID, PerfilAcesso> perfis = new HashMap<>();
        perfilRepository.listarTodos().forEach(p -> perfis.put(p.getId(), p));
        return usuarioRepository.listarTodos().stream()
                .map(u -> new UsuarioComPerfil(u, perfis.get(u.getPerfilId())))
                .toList();
    }
}
