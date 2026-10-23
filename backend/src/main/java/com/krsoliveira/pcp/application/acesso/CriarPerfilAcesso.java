package com.krsoliveira.pcp.application.acesso;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.application.comum.PermissoesDoUsuarioAtual;
import com.krsoliveira.pcp.domain.acesso.Delegacao;
import com.krsoliveira.pcp.domain.acesso.PerfilAcesso;
import com.krsoliveira.pcp.domain.acesso.PerfilAcessoRepository;
import com.krsoliveira.pcp.domain.acesso.Permissao;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Caso de uso: criar um perfil de acesso (ADR-0020). Só se concede o que se tem: as permissões do
 * perfil novo precisam estar entre as de quem o cria.
 */
public class CriarPerfilAcesso {

    private final PerfilAcessoRepository perfilRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public CriarPerfilAcesso(PerfilAcessoRepository perfilRepository, PermissoesDoUsuarioAtual permissoesAtuais,
                             ExecucaoAuditada execucao) {
        this.perfilRepository = perfilRepository;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public PerfilAcesso executar(String nome, String descricao, Collection<Permissao> permissoes) {
        return execucao.executar(ctx -> {
            PerfilAcesso perfil = PerfilAcesso.criar(nome, descricao, permissoes, ctx.usuario());
            Delegacao.exigirQueAbrange(permissoesAtuais.permissoes(), perfil.permissoesEfetivas());
            if (perfilRepository.buscarPorNome(perfil.getNome()).isPresent()) {
                throw new PerfilNomeJaUtilizadoException(perfil.getNome());
            }
            PerfilAcesso salvo = perfilRepository.salvar(perfil);
            ctx.registrar(TipoEntidade.PERFIL_ACESSO, salvo.getId(), salvo.getNome(), AcaoAuditoria.CRIADO,
                    Detalhes.com("nome", salvo.getNome())
                            .e("permissoes", nomes(salvo.permissoesEfetivas())));
            return salvo;
        });
    }

    static String nomes(Collection<Permissao> permissoes) {
        return permissoes.stream().map(Permissao::name).sorted().collect(Collectors.joining(", "));
    }
}
