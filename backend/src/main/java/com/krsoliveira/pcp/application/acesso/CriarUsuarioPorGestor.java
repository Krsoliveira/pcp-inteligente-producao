package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.auth.RegistrarUsuario;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.application.comum.UsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.usuario.PoliticaDeSenha;
import com.krsoliveira.pcp.domain.usuario.Usuario;

import java.util.UUID;

/**
 * Caso de uso: um gestor cria um usuário já com o perfil escolhido, e define a senha provisória
 * (ADR-0020). Só se concede o que se tem: o perfil escolhido não pode ter permissões que o gestor
 * não possui. O responsável registrado na trilha é o próprio gestor.
 */
public class CriarUsuarioPorGestor {

    private final PerfilAcessoRepository perfilRepository;
    private final RegistrarUsuario registrarUsuario;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final UsuarioAtual usuarioAtual;

    public CriarUsuarioPorGestor(PerfilAcessoRepository perfilRepository, RegistrarUsuario registrarUsuario,
                                 PermissoesDoUsuarioAtual permissoesAtuais, UsuarioAtual usuarioAtual) {
        this.perfilRepository = perfilRepository;
        this.registrarUsuario = registrarUsuario;
        this.permissoesAtuais = permissoesAtuais;
        this.usuarioAtual = usuarioAtual;
    }

    public Usuario executar(String nome, String email, String senhaProvisoria, UUID perfilId) {
        PerfilAcesso perfil = perfilRepository.buscarPorId(perfilId)
                .orElseThrow(() -> new PerfilNaoEncontradoException(perfilId));
        Delegacao.exigirQueAbrange(permissoesAtuais.permissoes(), perfil.permissoesEfetivas());
        PoliticaDeSenha.validar(senhaProvisoria);
        return registrarUsuario.executarComSenhaProvisoria(nome, email, senhaProvisoria, perfil, usuarioAtual.identificador());
    }
}
