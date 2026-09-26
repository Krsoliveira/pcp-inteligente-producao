package com.krsoliveira.pcp.application.ordem;

import com.krsoliveira.pcp.domain.ordem.TipoOrdem;
import com.krsoliveira.pcp.domain.ordem.TipoOrdemRepository;

import java.util.UUID;

/**
 * Caso de uso: atualizar os dados de um tipo de ordem existente.
 */
public class AtualizarTipoOrdem {

    private final TipoOrdemRepository tipoOrdemRepository;

    public AtualizarTipoOrdem(TipoOrdemRepository tipoOrdemRepository) {
        this.tipoOrdemRepository = tipoOrdemRepository;
    }

    public record Comando(UUID id, String nome, String descricao, String cor) {}

    public TipoOrdem executar(Comando comando) {
        TipoOrdem tipo = tipoOrdemRepository.buscarPorId(comando.id())
                .orElseThrow(() -> new TipoOrdemNaoEncontradoException(comando.id()));

        // Verifica unicidade do nome apenas se foi alterado
        if (!tipo.getNome().equalsIgnoreCase(comando.nome().trim())
                && tipoOrdemRepository.existePorNome(comando.nome().trim())) {
            throw new NomeTipoOrdemJaUtilizadoException(comando.nome());
        }

        tipo.atualizar(comando.nome(), comando.descricao(), comando.cor());
        return tipoOrdemRepository.salvar(tipo);
    }
}
