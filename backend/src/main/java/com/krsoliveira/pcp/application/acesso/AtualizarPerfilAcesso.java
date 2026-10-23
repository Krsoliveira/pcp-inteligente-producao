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
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: alterar nome, descrição e permissões de um perfil (ADR-0020). Quem altera precisa ter
 * todas as permissões que o perfil tem antes e que terá depois: ninguém mexe num perfil acima do seu
 * alcance nem o eleva além dele. A mudança entra na trilha com as permissões adicionadas e removidas.
 */
public class AtualizarPerfilAcesso {

    private final PerfilAcessoRepository perfilRepository;
    private final PermissoesDoUsuarioAtual permissoesAtuais;
    private final ExecucaoAuditada execucao;

    public AtualizarPerfilAcesso(PerfilAcessoRepository perfilRepository, PermissoesDoUsuarioAtual permissoesAtuais,
                                 ExecucaoAuditada execucao) {
        this.perfilRepository = perfilRepository;
        this.permissoesAtuais = permissoesAtuais;
        this.execucao = execucao;
    }

    public PerfilAcesso executar(UUID id, String nome, String descricao, Collection<Permissao> permissoes) {
        return execucao.executar(ctx -> {
            PerfilAcesso perfil = perfilRepository.buscarPorId(id)
                    .orElseThrow(() -> new PerfilNaoEncontradoException(id));
            String nomeAnterior = perfil.getNome();
            Set<Permissao> antes = copia(perfil.permissoesEfetivas());
            Set<Permissao> depois = copia(permissoes == null ? Set.of() : permissoes);

            // Tudo é conferido antes de alterar o perfil: uma recusa não deixa nada pela metade.
            perfil.exigirQuePodeSerAlterado();
            Set<Permissao> doAtor = permissoesAtuais.permissoes();
            Delegacao.exigirQueAbrange(doAtor, antes);
            Delegacao.exigirQueAbrange(doAtor, depois);
            String nomeNovo = nome == null ? "" : nome.trim();
            perfilRepository.buscarPorNome(nomeNovo)
                    .filter(outro -> !outro.getId().equals(perfil.getId()))
                    .ifPresent(outro -> { throw new PerfilNomeJaUtilizadoException(nomeNovo); });

            // Valida o conteúdo (nome, descrição, ao menos uma permissão) e só então altera.
            perfil.redefinir(nome, descricao, permissoes, ctx.usuario());

            PerfilAcesso salvo = perfilRepository.salvar(perfil);

            Set<Permissao> adicionadas = copia(depois);
            adicionadas.removeAll(antes);
            Set<Permissao> removidas = copia(antes);
            removidas.removeAll(depois);
            ctx.registrar(TipoEntidade.PERFIL_ACESSO, salvo.getId(), salvo.getNome(), AcaoAuditoria.ALTERADO,
                    Detalhes.com("perfil", salvo.getNome())
                            .mudanca("nome", nomeAnterior, salvo.getNome())
                            .e("permissoesAdicionadas", nuloSeVazio(adicionadas))
                            .e("permissoesRemovidas", nuloSeVazio(removidas)));
            return salvo;
        });
    }

    /** Cópia segura mesmo de um conjunto vazio ({@code EnumSet.copyOf} recusa coleção vazia). */
    private static Set<Permissao> copia(Collection<Permissao> permissoes) {
        Set<Permissao> copia = EnumSet.noneOf(Permissao.class);
        copia.addAll(permissoes);
        return copia;
    }

    /** Só entra nos detalhes da trilha o que mudou. */
    private static String nuloSeVazio(Collection<Permissao> permissoes) {
        return permissoes.isEmpty() ? null : CriarPerfilAcesso.nomes(permissoes);
    }
}
