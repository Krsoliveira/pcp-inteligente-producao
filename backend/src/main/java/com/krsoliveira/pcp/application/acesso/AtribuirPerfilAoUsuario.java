package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: trocar o perfil de acesso de um usuário (ADR-0020). Regras:
 * <ul>
 *   <li>ninguém altera o <b>próprio</b> perfil;</li>
 *   <li>só se concede o que se tem: o perfil novo e o atual do usuário precisam estar ao alcance de
 *       quem altera (não se rebaixa nem se promove alguém acima de si);</li>
 *   <li>o <b>último</b> usuário com o perfil Administrador não pode sair dele (o sistema ficaria sem
 *       quem gerencie acessos).</li>
 * </ul>
 */
public class AtribuirPerfilAoUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public AtribuirPerfilAoUsuario(UsuarioRepository usuarioRepository, PerfilAcessoRepository perfilRepository,
                                   PermissoesDoUsuarioAtual permissoesAtuais, ExecucaoAuditada execucao) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public Usuario executar(UUID usuarioId, UUID novoPerfilId) {
        return execucao.executar(ctx -> {
            Usuario usuario = usuarioRepository.porId(usuarioId)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
            PerfilAcesso novo = perfilRepository.buscarPorId(novoPerfilId)
                    .orElseThrow(() -> new PerfilNaoEncontradoException(novoPerfilId));
            PerfilAcesso atual = perfilRepository.buscarPorId(usuario.getPerfilId())
                    .orElseThrow(() -> new PerfilNaoEncontradoException(usuario.getPerfilId()));

            if (usuario.getEmail().equalsIgnoreCase(ctx.usuario())) {
                throw new AcessoNegadoException("Você não pode alterar o seu próprio perfil.");
            }
            Set<Permissao> doAtor = permissoesAtuais.permissoes();
            Delegacao.exigirQueAbrange(doAtor, atual.permissoesEfetivas());
            Delegacao.exigirQueAbrange(doAtor, novo.permissoesEfetivas());

            if (atual.getId().equals(novo.getId())) {
                return usuario;
            }
            if (atual.isAdministrador()) {
                exigirOutroAdministrador(atual);
            }

            usuario.atribuirPerfil(novo.getId());
            Usuario salvo = usuarioRepository.salvar(usuario);
            ctx.registrar(TipoEntidade.USUARIO, salvo.getId(), salvo.getEmail(), AcaoAuditoria.PERFIL_ATRIBUIDO,
                    Detalhes.com("email", salvo.getEmail()).mudanca("perfil", atual.getNome(), novo.getNome()));
            return salvo;
        });
    }

    /**
     * Bloqueia o perfil antes de contar. Sem isso, dois administradores rebaixando um ao outro ao mesmo tempo contam
     * dois, passam os dois, e o sistema fica sem nenhum.
     */
    private void exigirOutroAdministrador(PerfilAcesso administrador) {
        perfilRepository.buscarPorIdParaAtualizar(administrador.getId());
        if (usuarioRepository.contarPorPerfil(administrador.getId()) <= 1) {
            throw new RegraDeNegocioException(
                    "Este é o último usuário com o perfil Administrador; o sistema precisa de ao menos um.");
        }
    }
}
