package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.UUID;

/**
 * Casos de uso: desativar e reativar um usuário, sem excluí-lo (ADR-0020). Desativado não entra e perde a sessão aberta
 * na requisição seguinte, mas continua na trilha de auditoria e nas assinaturas dos registros.
 *
 * <p>Valem as regras de quem gerencia usuários: só alcança quem tem perfil ao alcance do gestor e nunca a si mesmo. O
 * sistema precisa de ao menos um Administrador ativo. O usuário é lido travado, então duas ações juntas não se sobrepõem.
 */
public class DesativacaoDeUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public DesativacaoDeUsuario(UsuarioRepository usuarioRepository, PerfilAcessoRepository perfilRepository,
                                PermissoesDoUsuarioAtual permissoesAtuais, ExecucaoAuditada execucao) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public void desativar(UUID usuarioId) {
        execucao.executar(ctx -> {
            Usuario alvo = alvoAoAlcance(usuarioId, ctx.usuario(), "Você não pode desativar o seu próprio usuário.");
            PerfilAcesso perfil = perfilDo(alvo);
            if (perfil.isAdministrador() && alvo.isAtivo()) {
                exigirOutroAdministradorAtivo(perfil);
            }

            alvo.desativar();
            usuarioRepository.salvar(alvo);
            ctx.registrar(TipoEntidade.USUARIO, alvo.getId(), alvo.getEmail(), AcaoAuditoria.USUARIO_DESATIVADO,
                    Detalhes.com("email", alvo.getEmail()).e("perfil", perfil.getNome()));
            return null;
        });
    }

    public void reativar(UUID usuarioId) {
        execucao.executar(ctx -> {
            Usuario alvo = alvoAoAlcance(usuarioId, ctx.usuario(), "Você não pode reativar o seu próprio usuário.");
            PerfilAcesso perfil = perfilDo(alvo);

            alvo.reativar();
            usuarioRepository.salvar(alvo);
            ctx.registrar(TipoEntidade.USUARIO, alvo.getId(), alvo.getEmail(), AcaoAuditoria.USUARIO_REATIVADO,
                    Detalhes.com("email", alvo.getEmail()).e("perfil", perfil.getNome()));
            return null;
        });
    }

    /** Lê o usuário travado e confere, antes de mudar qualquer coisa, que o gestor pode mexer nele. */
    private Usuario alvoAoAlcance(UUID usuarioId, String gestor, String recusaParaSiMesmo) {
        Usuario alvo = usuarioRepository.porIdParaAtualizar(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
        if (alvo.getEmail().equalsIgnoreCase(gestor)) {
            throw new AcessoNegadoException(recusaParaSiMesmo);
        }
        Delegacao.exigirQueAbrange(permissoesAtuais.permissoes(), perfilDo(alvo).permissoesEfetivas());
        return alvo;
    }

    private PerfilAcesso perfilDo(Usuario usuario) {
        return perfilRepository.buscarPorId(usuario.getPerfilId())
                .orElseThrow(() -> new PerfilNaoEncontradoException(usuario.getPerfilId()));
    }

    /** Trava o perfil Administrador, como na troca de perfil, para duas desativações juntas não zerarem os ativos. */
    private void exigirOutroAdministradorAtivo(PerfilAcesso administrador) {
        perfilRepository.buscarPorIdParaAtualizar(administrador.getId());
        if (usuarioRepository.contarAtivosPorPerfil(administrador.getId()) <= 1) {
            throw new RegraDeNegocioException(
                    "Este é o último Administrador ativo; o sistema precisa de ao menos um.");
        }
    }
}
