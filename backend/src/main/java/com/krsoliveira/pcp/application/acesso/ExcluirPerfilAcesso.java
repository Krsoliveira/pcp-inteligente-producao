package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.RegraDeNegocioException;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.UUID;

/**
 * Caso de uso: excluir um perfil de acesso (ADR-0020). Só perfis que não são de sistema e que nenhum
 * usuário usa; e só por quem tem todas as permissões do perfil (não se mexe acima do próprio alcance).
 */
public class ExcluirPerfilAcesso {

    private final PerfilAcessoRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public ExcluirPerfilAcesso(PerfilAcessoRepository perfilRepository, UsuarioRepository usuarioRepository,
                               PermissoesDoUsuarioAtual permissoesAtuais, ExecucaoAuditada execucao) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public void executar(UUID id) {
        execucao.executar(ctx -> {
            PerfilAcesso perfil = perfilRepository.buscarPorId(id)
                    .orElseThrow(() -> new PerfilNaoEncontradoException(id));
            if (!perfil.podeSerExcluido()) {
                throw new RegraDeNegocioException(
                        "O perfil %s é de sistema e não pode ser excluído.".formatted(perfil.getNome()));
            }
            Delegacao.exigirQueAbrange(permissoesAtuais.permissoes(), perfil.permissoesEfetivas());
            long emUso = usuarioRepository.contarPorPerfil(id);
            if (emUso > 0) {
                throw new RegraDeNegocioException(
                        "O perfil %s está atribuído a %d %s. Troque o perfil desses usuários antes de excluí-lo."
                                .formatted(perfil.getNome(), emUso, emUso == 1 ? "usuário" : "usuários"));
            }
            perfilRepository.excluir(id);
            ctx.registrar(TipoEntidade.PERFIL_ACESSO, id, perfil.getNome(), AcaoAuditoria.EXCLUIDO,
                    Detalhes.com("nome", perfil.getNome()));
            return null;
        });
    }
}
