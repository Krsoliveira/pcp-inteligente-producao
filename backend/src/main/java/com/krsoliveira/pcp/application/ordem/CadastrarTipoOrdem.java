package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

/**
 * Caso de uso: cadastrar uma nova categoria de ordem de produção.
 *
 * Exemplos de uso: Produção Normal, Manutenção, Revenda, Retrabalho.
 * O campo {@code cor} (hex) é usado pelo frontend para exibir badges coloridos.
 */
public class CadastrarTipoOrdem {

    private final TipoOrdemRepository tipoOrdemRepository;

    public CadastrarTipoOrdem(TipoOrdemRepository tipoOrdemRepository) {
        this.tipoOrdemRepository = tipoOrdemRepository;
    }

    public record Comando(String nome, String descricao, String cor) {}

    public TipoOrdem executar(Comando comando) {
        if (tipoOrdemRepository.existePorNome(comando.nome().trim())) {
            throw new NomeTipoOrdemJaUtilizadoException(comando.nome());
        }
        TipoOrdem tipo = TipoOrdem.criar(comando.nome(), comando.descricao(), comando.cor());
        return tipoOrdemRepository.salvar(tipo);
    }
}
