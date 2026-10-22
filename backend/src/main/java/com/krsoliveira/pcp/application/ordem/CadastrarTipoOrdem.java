package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.application.comum.Detalhes;
import com.krsoliveira.pcp.application.comum.ExecucaoAuditada;
import com.krsoliveira.pcp.domain.auditoria.AcaoAuditoria;
import com.krsoliveira.pcp.domain.auditoria.TipoEntidade;
import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

/** Caso de uso: cadastrar um tipo de ordem (categoria definida pelo usuário). */
public class CadastrarTipoOrdem {

    private final TipoOrdemRepository tipoOrdemRepository;
    private final ExecucaoAuditada execucao;

    public CadastrarTipoOrdem(TipoOrdemRepository tipoOrdemRepository, ExecucaoAuditada execucao) {
        this.tipoOrdemRepository = tipoOrdemRepository;
        this.execucao = execucao;
    }

    public record Comando(String nome, String descricao, String cor) {}

    public TipoOrdem executar(Comando comando) {
        return execucao.executar(ctx -> {
            if (tipoOrdemRepository.existePorNome(comando.nome().trim())) {
                throw new NomeTipoOrdemJaUtilizadoException(comando.nome());
            }
            TipoOrdem tipo = tipoOrdemRepository.salvar(
                    TipoOrdem.criar(comando.nome(), comando.descricao(), comando.cor(), ctx.usuario()));
            ctx.registrar(TipoEntidade.TIPO_ORDEM, tipo.getId(), tipo.getNome(), AcaoAuditoria.CRIADO,
                    Detalhes.com("nome", tipo.getNome())
                            .e("descricao", tipo.getDescricao())
                            .e("cor", tipo.getCor()));
            return tipo;
        });
    }
}
