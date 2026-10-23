package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.AcessoNegadoException;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.usuario.CodificadorDeSenha;
import com.krsoliveira.pcp.domain.usuario.PoliticaDeSenha;
import com.krsoliveira.pcp.domain.usuario.Usuario;
import com.krsoliveira.pcp.domain.usuario.UsuarioRepository;

import java.util.UUID;

/**
 * Caso de uso: um gestor redefine a senha de um usuário, que passa a ter uma senha provisória e precisa
 * trocá-la no próximo acesso (ADR-0021). É o caminho de quem esqueceu a senha.
 *
 * Quem redefine só alcança quem está ao seu alcance: o perfil do alvo não pode ter permissões que o gestor
 * não tem. Sem isso, um gerente redefiniria a senha de um Administrador e assumiria a conta dele. Ninguém
 * redefine a própria senha por aqui (usa "Alterar minha senha", que exige a atual).
 */
public class RedefinirSenhaDoUsuario {

    private final UsuarioRepository usuarioRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final CodificadorDeSenha codificador;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public RedefinirSenhaDoUsuario(UsuarioRepository usuarioRepository, PerfilAcessoRepository perfilRepository,
                                   CodificadorDeSenha codificador, PermissoesDoUsuarioAtual permissoesAtuais,
                                   ExecucaoAuditada execucao) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.codificador = codificador;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public void executar(UUID usuarioId, String senhaProvisoria) {
        execucao.executar(ctx -> {
            Usuario alvo = usuarioRepository.porId(usuarioId)
                    .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));
            PerfilAcesso perfilDoAlvo = perfilRepository.buscarPorId(alvo.getPerfilId())
                    .orElseThrow(() -> new PerfilNaoEncontradoException(alvo.getPerfilId()));

            if (alvo.getEmail().equalsIgnoreCase(ctx.usuario())) {
                throw new AcessoNegadoException(
                        "Para trocar a sua própria senha use \"Alterar minha senha\", que pede a senha atual.");
            }
            Delegacao.exigirQueAbrange(permissoesAtuais.permissoes(), perfilDoAlvo.permissoesEfetivas());
            PoliticaDeSenha.validar(senhaProvisoria);

            alvo.redefinirSenha(codificador.codificar(senhaProvisoria));
            usuarioRepository.salvar(alvo);
            ctx.registrar(TipoEntidade.USUARIO, alvo.getId(), alvo.getEmail(), AcaoAuditoria.SENHA_REDEFINIDA,
                    Detalhes.vazio());
            return null;
        });
    }
}
