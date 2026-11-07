package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.EdicaoConcorrenteException;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: editar um tipo de ordem. A auditoria registra cada campo alterado (de → para).
 *
 * O tipo é lido travado e a versão que a tela leu precisa ser a atual: duas edições simultâneas não se
 * sobrepõem em silêncio, a segunda recebe 409 (ADR-0034).
 */
public class AtualizarTipoOrdem {

    private final TipoOrdemRepository tipoOrdemRepository;
    private final ExecucaoAuditada execucao;

    public AtualizarTipoOrdem(TipoOrdemRepository tipoOrdemRepository, ExecucaoAuditada execucao) {
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.execucao = execucao;
    }

    /** @param versao versão do tipo quando a tela abriu a edição; {@code null} dispensa a conferência */
    public record Comando(UUID id, String nome, String descricao, String cor, Long versao) {}

    public TipoOrdem executar(Comando comando) {
        return execucao.executar(ctx -> {
            TipoOrdem tipo = tipoOrdemRepository.buscarPorIdParaAtualizar(comando.id())
                    .orElseThrow(() -> new TipoOrdemNaoEncontradoException(comando.id()));
            EdicaoConcorrenteException.exigirVersaoAtual(comando.versao(), tipo.getVersao(),
                    "O tipo de ordem \"%s\"".formatted(tipo.getNome()));

            // Verifica unicidade do nome apenas se foi alterado
            if (!tipo.getNome().equalsIgnoreCase(comando.nome().trim())
                    && tipoOrdemRepository.existePorNome(comando.nome().trim())) {
                throw new NomeTipoOrdemJaUtilizadoException(comando.nome());
            }

            String nomeAntes = tipo.getNome();
            String descricaoAntes = tipo.getDescricao();
            String corAntes = tipo.getCor();

            tipo.atualizar(comando.nome(), comando.descricao(), comando.cor(), ctx.usuario());
            TipoOrdem salvo = tipoOrdemRepository.salvar(tipo);

            Detalhes detalhes = Detalhes.com("nome", salvo.getNome());
            if (!Objects.equals(nomeAntes, salvo.getNome())) {
                detalhes.mudanca("nome", nomeAntes, salvo.getNome());
            }
            if (!Objects.equals(descricaoAntes, salvo.getDescricao())) {
                detalhes.mudanca("descricao", descricaoAntes, salvo.getDescricao());
            }
            if (!Objects.equals(corAntes, salvo.getCor())) {
                detalhes.mudanca("cor", corAntes, salvo.getCor());
            }
            ctx.registrar(TipoEntidade.TIPO_ORDEM, salvo.getId(), salvo.getNome(),
                    AcaoAuditoria.ALTERADO, detalhes);
            return salvo;
        });
    }
}
